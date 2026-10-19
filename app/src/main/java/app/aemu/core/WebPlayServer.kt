/* Added for AEmulator X, 2026-10-19: play the running system in a web browser. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.Executors

/**
 * A small HTTP server on the local network: a browser that opens http://<phone>:<port>/?k=<code> sees the guest
 * screen (JPEG frames, fetched one after another) and drives it with the mouse or a finger, the keyboard and
 * Back / Home / Recents buttons. Every request must carry the access code [token]; nothing else is served.
 */
class WebPlayServer(
    private val title: String,
    val token: String,
    private val onTouch: (action: String, x: Int, y: Int) -> Unit,
    private val onKey: (keysym: Int, down: Boolean) -> Unit,
    private val onButton: (name: String) -> Unit,
    private val log: (String) -> Unit = {},
) {
    @Volatile private var server: ServerSocket? = null
    @Volatile private var frame: ByteArray? = null
    @Volatile private var lastFrameAsk = 0L
    private val pool = Executors.newFixedThreadPool(4)

    /** True while a browser has asked for frames in the last 2 s (grab fast then, slowly otherwise). */
    val watched: Boolean get() = System.currentTimeMillis() - lastFrameAsk < 2000
    val port: Int get() = server?.localPort ?: 0

    fun submitFrame(jpeg: ByteArray) { frame = jpeg }

    /** Listens on [preferred] or the next free port up to +10; returns the port. */
    fun start(preferred: Int = 8080): Int {
        var s: ServerSocket? = null
        for (p in preferred..preferred + 10) { s = runCatching { ServerSocket(p, 16, InetAddress.getByName("0.0.0.0")) }.getOrNull(); if (s != null) break }
        val ss = s ?: error("no free port from $preferred")
        server = ss
        Thread({
            while (server === ss) {
                val c = runCatching { ss.accept() }.getOrNull() ?: break
                pool.execute { runCatching { c.use { handle(it) } } }
            }
        }, "aemu-web").apply { isDaemon = true; start() }
        log("web play: listening on port ${ss.localPort}")
        return ss.localPort
    }

    fun stop() {
        val s = server; server = null
        runCatching { s?.close() }
        pool.shutdownNow()
    }

    private fun handle(c: Socket) {
        c.soTimeout = 5000
        val input = BufferedInputStream(c.getInputStream())
        val line = readLine(input) ?: return
        while (true) { val h = readLine(input) ?: break; if (h.isEmpty()) break }   // headers are not needed
        val target = line.split(' ').getOrNull(1) ?: return
        val path = target.substringBefore('?')
        val q = query(target.substringAfter('?', ""))
        val out = c.getOutputStream()
        if (q["k"] != token) { reply(out, 403, "text/plain", "Forbidden: open the address shown in AEmulator X".toByteArray()); return }
        when (path) {
            "/" -> reply(out, 200, "text/html; charset=utf-8", WebPlayPage.html(title, token).toByteArray())
            "/frame.jpg" -> {
                lastFrameAsk = System.currentTimeMillis()
                val f = frame
                if (f == null) reply(out, 503, "text/plain", "no frame yet".toByteArray()) else reply(out, 200, "image/jpeg", f)
            }
            "/touch" -> { onTouch(q["a"].orEmpty(), q["x"]?.toIntOrNull() ?: 0, q["y"]?.toIntOrNull() ?: 0); reply(out, 204, "text/plain", ByteArray(0)) }
            "/key" -> { q["s"]?.let { keysym(it) }?.let { onKey(it, q["d"] == "1") }; reply(out, 204, "text/plain", ByteArray(0)) }
            "/btn" -> { onButton(q["b"].orEmpty()); reply(out, 204, "text/plain", ByteArray(0)) }
            else -> reply(out, 404, "text/plain", "not found".toByteArray())
        }
    }

    private fun reply(out: OutputStream, code: Int, type: String, body: ByteArray) {
        val reason = mapOf(200 to "OK", 204 to "No Content", 403 to "Forbidden", 404 to "Not Found", 503 to "Service Unavailable")[code] ?: "OK"
        out.write(("HTTP/1.1 $code $reason\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray())
        out.write(body); out.flush()
    }

    private fun readLine(i: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (sb.length < 4096) {
            val b = i.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    companion object {
        fun query(s: String): Map<String, String> = s.split('&').filter { '=' in it }.associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }

        fun newToken(): String {
            val r = SecureRandom(); val abc = "abcdefghjkmnpqrstuvwxyz23456789"
            return (1..8).map { abc[r.nextInt(abc.length)] }.joinToString("")
        }

        /** Browser KeyboardEvent.key → X11 keysym (what [VncKeys] maps); null for keys the guest has no use for. */
        fun keysym(key: String): Int? = when {
            key.length == 1 -> key[0].code.takeIf { it in 0x20..0x7e }
            else -> when (key) {
                "Enter" -> 0xff0d; "Backspace" -> 0xff08; "Tab" -> 0xff09; "Escape" -> 0xff1b; "Delete" -> 0xffff
                "ArrowLeft" -> 0xff51; "ArrowUp" -> 0xff52; "ArrowRight" -> 0xff53; "ArrowDown" -> 0xff54
                "Home" -> 0xff50; "F1" -> 0xffbe; "Shift" -> 0xffe1; "Control" -> 0xffe3; "Alt" -> 0xffe9
                else -> null
            }
        }
    }
}

/** The single page the browser gets. */
object WebPlayPage {
    fun html(title: String, token: String): String {
        val t = title.replace("&", "&amp;").replace("<", "&lt;")
        return """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>$t</title><style>
body{margin:0;background:#0b0b12;color:#eee;font-family:system-ui,sans-serif;display:flex;flex-direction:column;align-items:center;height:100vh}
header{padding:8px 12px;font-weight:600}
#s{flex:1;min-height:0;max-width:100%;object-fit:contain;touch-action:none;cursor:pointer;border-radius:12px;background:#000}
nav{display:flex;gap:10px;padding:10px}
button{background:#5637cf;color:#fff;border:0;border-radius:20px;padding:10px 18px;font-size:16px;cursor:pointer}
small{opacity:.6;padding-bottom:8px}
</style></head><body><header>$t · AEmulator X</header>
<img id="s" alt="screen" draggable="false">
<nav><button data-b="back">◀ Back</button><button data-b="home">● Home</button><button data-b="recents">■ Recents</button></nav>
<small>Mouse or finger to touch · your keyboard types · Esc = Back</small>
<script>
const k=${'"'}$token${'"'},img=document.getElementById('s');
async function loop(){for(;;){try{const r=await fetch('frame.jpg?k='+k+'&t='+Date.now());if(r.ok){const u=URL.createObjectURL(await r.blob());const o=img.src;img.src=u;if(o.startsWith('blob:'))URL.revokeObjectURL(o)}else await new Promise(z=>setTimeout(z,500))}catch(e){await new Promise(z=>setTimeout(z,1000))}}}
function pos(e){const b=img.getBoundingClientRect(),w=img.naturalWidth,h=img.naturalHeight;if(!w)return null;
const s=Math.min(b.width/w,b.height/h),ox=b.left+(b.width-w*s)/2,oy=b.top+(b.height-h*s)/2;
return[Math.round((e.clientX-ox)/s),Math.round((e.clientY-oy)/s)]}
let down=false;
function send(a,e){const p=pos(e);if(!p)return;fetch('touch?k='+k+'&a='+a+'&x='+p[0]+'&y='+p[1])}
img.addEventListener('pointerdown',e=>{down=true;img.setPointerCapture(e.pointerId);send('down',e)});
img.addEventListener('pointermove',e=>{if(down)send('move',e)});
img.addEventListener('pointerup',e=>{if(down){down=false;send('up',e)}});
document.querySelectorAll('button').forEach(b=>b.onclick=()=>fetch('btn?k='+k+'&b='+b.dataset.b));
const keys={Enter:1,Backspace:1,Tab:1,Escape:1,Delete:1,ArrowLeft:1,ArrowUp:1,ArrowRight:1,ArrowDown:1,Home:1,F1:1,Shift:1,Control:1,Alt:1};
function key(e,d){if(e.key.length!==1&&!keys[e.key])return;e.preventDefault();fetch('key?k='+k+'&d='+d+'&s='+encodeURIComponent(e.key))}
document.addEventListener('keydown',e=>key(e,1));document.addEventListener('keyup',e=>key(e,0));
loop();
</script></body></html>"""
    }
}
