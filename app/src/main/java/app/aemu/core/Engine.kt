package app.aemu.core

/**
 * Набор движка: хостовые исполняемые файлы (лежат в nativeLibraryDir как lib*.so)
 * и гостевые файлы (32-битный ARM, копируются в дерево прошивки из assets/engines/<id>).
 *
 * GB — Android 2.x (Gingerbread/Froyo): GL рисуется в pbuffer и читается в fb0, binder и GL-сервер
 *      живут в отдельных процессах-«слотах» приложения.
 * KK — Android 4.x и новее: GL-мост работает прямо в процессе приложения и рисует в Surface.
 */
enum class Engine(
    val id: String,
    val title: String,
    val qemu: String,
    /** same translator with the guest mmap base moved off 0x40000000 (see [QemuFallback]) */
    val qemuXperiaPlay: String,
    val binderd: String,
    val glserverd: String,
    val runner: String,
    /** гостевая реализация EGL/GLES: имя в assets → имя в /system/lib/egl */
    val guestGles: String,
) {
    GB(
        id = "gb",
        title = "Fallback for 2.x",
        qemu = "libqemu_gb.so",
        qemuXperiaPlay = "libqemu_gb_xperiaplay.so",
        binderd = "libbinderd_gb.so",
        glserverd = "libglserverd_gb.so",
        runner = "libdhdrun_gb.so",
        guestGles = "libGLES_dhd.so",
    ),
    KK(
        id = "kk",
        title = "Universal (2.2–7.x)",
        qemu = "libqemu_kk.so",
        qemuXperiaPlay = "libqemu_kk_xperiaplay.so",
        binderd = "libbinderd_kk.so",
        glserverd = "libglserverd_kk.so",
        runner = "libdhdrun_kk.so",
        guestGles = "libGLES.so",
    );

    /**
     * Standalone GL server for this host GPU. On Mali the server must not put an app's ES2+ context into the share
     * group of SurfaceFlinger's ES1 context: the driver then hands the app an ES 1.1 context, no shader compiles and
     * hwui aborts in a loop (5.0+). The *_mali build only shares ES1 contexts with each other.
     */
    fun glserverdFor(gpu: String): String =
        if (this == KK && gpu.contains("mali")) "libglserverd_kk_mali.so" else glserverd

    companion object {
        /**
         * KK — универсальный: GL-мост в приложении (быстрее) и полный GLES 1/2/3, работает и с 2.2.
         * GB остаётся запасным (выбирается в настройках образа).
         */
        fun forApi(api: Int): Engine = KK
        fun byId(id: String?): Engine = entries.firstOrNull { it.id == id } ?: KK
    }
}
