package de.pulselibre

/**
 * Hand-drawn point data (viewBox 1200 x 370) for the header mountain range Eiger - Moench - Jungfrau, from left to right.
 * Derived from our own schematic study `design/header/eiger-moench-jungfrau-silhouette-study.svg`; no photo or third-party
 * asset is embedded. Coordinates are x,y pairs; the drawable scales them uniformly (no distortion) and roughens the edges.
 */
internal object HeaderMountainData {
    const val WIDTH = 1200f
    const val HEIGHT = 370f
    /** Peak positions (x) of Eiger, Moench, Jungfrau, used to centre the trio on narrow screens. */
    const val EIGER_X = 206f
    const val MOENCH_X = 438f
    const val JUNGFRAU_X = 814f
    const val TOP_Y = 107f

    val main = floatArrayOf(0f, 290f, 75f, 264f, 132f, 230f, 171f, 190f, 206f, 144f, 234f, 135f, 253f, 160f, 280f, 182f, 302f, 205f, 333f, 213f, 372f, 187f, 405f, 150f, 438f, 115f, 462f, 127f, 492f, 157f, 515f, 177f, 550f, 194f, 587f, 203f, 617f, 184f, 649f, 160f, 679f, 149f, 709f, 164f, 746f, 153f, 780f, 128f, 814f, 107f, 847f, 121f, 873f, 136f, 902f, 151f, 930f, 145f, 963f, 169f, 990f, 178f, 1020f, 199f, 1054f, 197f, 1093f, 219f, 1140f, 220f, 1200f, 244f)
    val mid = floatArrayOf(0f, 329f, 110f, 304f, 197f, 321f, 306f, 282f, 380f, 310f, 472f, 278f, 578f, 321f, 679f, 285f, 787f, 315f, 900f, 286f, 998f, 312f, 1094f, 286f, 1200f, 303f)
    val near = floatArrayOf(0f, 354f, 133f, 329f, 237f, 349f, 350f, 323f, 464f, 346f, 571f, 323f, 690f, 347f, 804f, 327f, 910f, 350f, 1020f, 325f, 1200f, 342f)
    /** Light rock ribs running down from the peaks (each polyline separately). */
    val ribs = listOf(floatArrayOf(206f, 144f, 220f, 163f, 231f, 160f, 247f, 181f, 252f, 195f, 269f, 210f), floatArrayOf(206f, 144f, 197f, 174f, 181f, 193f, 178f, 214f), floatArrayOf(438f, 115f, 425f, 148f, 417f, 163f, 395f, 183f), floatArrayOf(438f, 115f, 451f, 142f, 466f, 148f, 481f, 176f, 494f, 188f), floatArrayOf(814f, 107f, 799f, 130f, 782f, 141f, 775f, 162f), floatArrayOf(814f, 107f, 834f, 139f, 844f, 152f, 865f, 162f), floatArrayOf(873f, 136f, 894f, 170f, 907f, 188f))
    /** Dark shadow couloirs. */
    val shades = listOf(floatArrayOf(235f, 157f, 228f, 197f, 248f, 216f, 244f, 246f), floatArrayOf(196f, 178f, 204f, 219f, 189f, 237f), floatArrayOf(427f, 144f, 439f, 183f, 421f, 221f), floatArrayOf(465f, 156f, 453f, 187f, 468f, 227f), floatArrayOf(788f, 138f, 805f, 171f, 797f, 205f), floatArrayOf(844f, 150f, 829f, 182f, 844f, 222f), floatArrayOf(905f, 161f, 925f, 202f, 916f, 226f))
}
