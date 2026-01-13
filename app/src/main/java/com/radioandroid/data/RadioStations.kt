package com.radioandroid.data

import com.radioandroid.R

/**
 * List of popular Spanish radio stations with verified streaming URLs
 * URLs from TDTChannels GitHub project (https://github.com/LaQuay/TDTChannels)
 * Updated January 2025
 */
object RadioStations {
    val stations = listOf(
        RadioStation(
            id = 1,
            name = "Los 40",
            // Official AAC Stream (StreamTheWorld)
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40AAC.aac",
            logoResId = R.drawable.logo_los40,
            genre = "Música",
            country = "España"
        ),
        // TEST STATION - URL ROTA (para verificar auto-skip)
        // Esta emisora se usa para verificar que el sistema detecta URLs rotas
        // y salta automáticamente a la siguiente emisora
        RadioStation(
            id = 999,
            name = "🔧 TEST - Enlace Roto",
            streamUrl = "http://127.0.0.1:54321/stream", // Connection refused (Instant failure)
            logoResId = R.drawable.logo_los40, // Reutilizar icono existente
            genre = "Test",
            country = "España"
        ),
        RadioStation(
            id = 2,
            name = "Cadena SER",
            // TDTChannels verified
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3",
            logoResId = R.drawable.logo_ser,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 3,
            name = "COPE",
            // Verified Official MP3 (Flumotion)
            streamUrl = "https://flucast09-h-cloud.flumotion.com/cope/net1.mp3",
            logoResId = R.drawable.logo_cope,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 4,
            name = "Cadena 100",
            // Verified Official MP3 (Flumotion)
            streamUrl = "https://flucast09-h-cloud.flumotion.com/cope/cadena100.mp3",
            logoResId = R.drawable.logo_cadena100,
            genre = "Música"
        ),
        RadioStation(
            id = 5,
            name = "Europa FM",
            // StreamTheWorld AAC stream - now handled by IcyDataSource
            streamUrl = "https://20043.live.streamtheworld.com/EFMAAC.aac",
            logoResId = R.drawable.logo_europafm,
            genre = "Música"
        ),
        RadioStation(
            id = 6,
            name = "Rock FM",
            // Verified Official MP3 (Flumotion)
            streamUrl = "https://flucast09-h-cloud.flumotion.com/cope/rockfm.mp3",
            logoResId = R.drawable.logo_rockfm,
            genre = "Música"
        ),
        RadioStation(
            id = 7,
            name = "Cadena Dial",
            // Official AAC Stream (StreamTheWorld)
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIALAAC.aac",
            logoResId = R.drawable.logo_cadenadial,
            genre = "Música"
        ),
        RadioStation(
            id = 8,
            name = "Kiss FM",
            // Verified from kissfm.es official player
            streamUrl = "https://bbkissfm.kissfmradio.cires21.com/bbkissfm.mp3",
            logoResId = R.drawable.logo_kissfm,
            genre = "Música"
        ),
        RadioStation(
            id = 9,
            name = "Onda Cero",
            // TDTChannels verified - Atresmedia AAC stream
            streamUrl = "https://radio-atres-live.ondacero.es/api/livestream-redirect/OCAAC.aac",
            logoResId = R.drawable.logo_ondacero,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 10,
            name = "RAC1",
            // TDTChannels verified
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RAC_1.mp3",
            logoResId = R.drawable.logo_rac1,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 11,
            name = "LOS40 Dance",
            // TDTChannels verified
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_DANCE.mp3",
            logoResId = R.drawable.logo_los40_dance,
            genre = "Música"
        ),
        RadioStation(
            id = 12,
            name = "RNE Radio Nacional",
            // Source: Official RTVE streaming infrastructure
            // Format: MP3 (Standard stable stream) | Updated: 2026-01-10
            streamUrl = "https://dispatcher.rndfnk.com/crtve/rne1/nav/mp3/high",
            logoResId = R.drawable.logo_rne,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 13,
            name = "RNE Radio 3",
            streamUrl = "http://dispatcher.rndfnk.com/crtve/rner3/main/mp3/high",
            logoResId = R.drawable.logo_rne_radio3,
            genre = "Música"
        ),
        RadioStation(
            id = 14,
            name = "RNE Radio 5",
            streamUrl = "https://dispatcher.rndfnk.com/crtve/rne1/bal/mp3/high",
            logoResId = R.drawable.logo_rne_radio5,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 15,
            name = "RNE Radio Clásica",
            streamUrl = "http://dispatcher.rndfnk.com/crtve/rnerc/main/mp3/high",
            logoResId = R.drawable.logo_rne_clasica,
            genre = "Música Clásica"
        ),
        RadioStation(
            id = 16,
            name = "Onda Melodía",
            streamUrl = "https://radio-atres-live.ondacero.es/api/livestream-redirect/MELODIA_FMAAC.aac",
            logoResId = R.drawable.logo_onda_melodia,
            genre = "Música"
        ),
        RadioStation(
            id = 17,
            name = "LOS40 Classic",
            // Official AAC Stream (StreamTheWorld)
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_CLASSICAAC.aac",
            logoResId = R.drawable.logo_los40_classic,
            genre = "Música"
        ),
        RadioStation(
            id = 18,
            name = "LOS40 Urban",
            // Official AAC Stream (StreamTheWorld)
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_URBANAAC.aac",
            logoResId = R.drawable.logo_los40_urban,
            genre = "Música"
        ),
        RadioStation(
            id = 19,
            name = "Radiolé",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOLE.mp3",
            logoResId = R.drawable.logo_radiole,
            genre = "Música"
        ),
        RadioStation(
            id = 20,
            name = "MegaStar FM",
            // Verified Official MP3 (Flumotion)
            streamUrl = "https://flucast09-h-cloud.flumotion.com/cope/megastar.mp3",
            logoResId = R.drawable.logo_megastar,
            genre = "Música"
        ),
        RadioStation(
            id = 21,
            name = "esRadio",
            streamUrl = "https://libertaddigital-radio-live1.flumotion.com/libertaddigital/ld-live1-low.mp3",
            logoResId = R.drawable.logo_esradio,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 22,
            name = "Radio Marca",
            // Fixed: Moved to StreamTheWorld (Icecast was unstable)
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOMARCA_NACIONAL.mp3",
            logoResId = R.drawable.logo_radiomarca,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 23,
            name = "Catalunya Ràdio",
            streamUrl = "https://shoutcast.ccma.cat/ccma/catalunyaradioHD.mp3",
            logoResId = R.drawable.logo_catalunya_radio,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 24,
            name = "Canal Sur Radio",
            streamUrl = "https://rtva-live-radio.flumotion.com/rtva/csralg.mp3",
            logoResId = R.drawable.logo_canal_sur,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 25,
            name = "Canal Fiesta",
            // Verified from emisora.org.es - MP3 direct stream
            streamUrl = "https://rtva-live-radio.flumotion.com/rtva/cfr.mp3",
            logoResId = R.drawable.logo_canal_fiesta,
            genre = "Música"
        ),
        RadioStation(
            id = 26,
            name = "Euskadi Irratia",
            // Fixed: Switched to standard MP3 stream (more stable/compatible than AAC)
            streamUrl = "https://mp3-eitb.stream.flumotion.com/eitb/euskadiirratia.mp3",
            logoResId = R.drawable.logo_euskadi_irratia,
            genre = "Noticias/Voz"
        ),
        RadioStation(
            id = 27,
            name = "Gaztea",
            streamUrl = "https://eitb-gaztea.flumotion.com/eitb/gaztea.mp3",
            logoResId = R.drawable.logo_gaztea,
            genre = "Música"
        )
    ) + getInternationalStations()

    private fun getInternationalStations(): List<RadioStation> {
        return listOf(
            RadioStation(
                id = 101,
                name = "BBC Radio 1",
                // Updated to HTTPS worldwide stream (more reliable)
                streamUrl = "https://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d96000.norewind.m3u8",
                logoResId = R.drawable.logo_bbc_radio1,
                genre = "Música",
                country = "United Kingdom"
            ),
            RadioStation(
                id = 102,
                name = "Capital FM London",
                streamUrl = "https://media-ice.musicradio.com/CapitalMP3",
                logoResId = R.drawable.logo_capital_fm,
                genre = "Música",
                country = "United Kingdom"
            ),
            RadioStation(
                id = 103,
                name = "Classic FM",
                streamUrl = "https://media-ice.musicradio.com/ClassicFMMP3",
                logoResId = R.drawable.logo_classic_fm,
                genre = "Música Clásica",
                country = "United Kingdom"
            ),
            // Mexico - Verified AAC/MP3 streams (tested 2026-01-13)
            // W Radio México - NO PUBLIC STREAM AVAILABLE (404 on all known URLs)
            /*
            RadioStation(
                id = 201,
                name = "W Radio México",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XEWFMAAC.aac",
                logoResId = R.drawable.logo_w_radio, 
                genre = "Noticias/Voz",
                country = "México"
            ),
            */
            RadioStation(
                id = 202,
                name = "Los 40 México",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_MEXICOAAC.aac",
                logoResId = R.drawable.logo_los40_mx,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 203,
                name = "EXA FM",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHEXAAAC.aac",
                logoResId = R.drawable.logo_exa_fm,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 204,
                name = "La Mejor",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHTIMAAC.aac",
                logoResId = R.drawable.logo_la_mejor,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 205,
                name = "Amor FM",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHSHFMAAC.aac",
                logoResId = R.drawable.logo_amor_fm,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 206,
                name = "MVS Noticias",
                streamUrl = "https://20593.live.streamtheworld.com:443/XHMVSFM_SC",
                logoResId = R.drawable.logo_mvs_noticias,
                genre = "Noticias/Voz",
                country = "México"
            ),
            RadioStation(
                id = 207,
                name = "Alfa 91.3",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHFAJ_FMAAC.aac",
                logoResId = R.drawable.logo_alfa_913,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 208,
                name = "Beat 100.9",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHSONFMAAC.aac",
                logoResId = R.drawable.logo_beat_1009,
                genre = "Música Electrónica",
                country = "México"
            ),
            RadioStation(
                id = 209,
                name = "Universal Stereo",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHRED_FM.mp3",
                logoResId = R.drawable.logo_universal,
                genre = "Clásicos",
                country = "México"
            ),
            RadioStation(
                id = 210,
                name = "Stereo Joya",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XEJP_FM.mp3",
                logoResId = R.drawable.logo_joya,
                genre = "Pop/Balada",
                country = "México"
            ),
            RadioStation(
                id = 212,
                name = "Ke Buena Guadalajara",
                // Using GDL stream - CDMX stream (XEQFMAAC) returns 404
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/KEBUENA_GDLAAC.aac",
                logoResId = R.drawable.logo_ke_buena,
                genre = "Regional Mexicano",
                country = "México"
            ),
            // Reactor 105.7 - Server unreachable
            /*
            RadioStation(
                id = 213,
                name = "Reactor 105.7",
                streamUrl = "http://s1.mexside.net:8002/stream",
                logoResId = R.drawable.logo_reactor,
                genre = "Rock/Alternativo",
                country = "México"
            ),
            */
            // Radio Fórmula - Stream returns 404
            /*
            RadioStation(
                id = 214,
                name = "Radio Fórmula 103.3",
                streamUrl = "http://stream.radiojar.com/3zcuxdmb4k0uv.mp3",
                logoResId = R.drawable.logo_formula,
                genre = "Noticias/Voz",
                country = "México"
            ),
            */
            RadioStation(
                id = 215,
                name = "Mix 106.5",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHDFMFM.mp3",
                logoResId = R.drawable.logo_mix,
                genre = "80s & 90s",
                country = "México"
            ),
            RadioStation(
                id = 216,
                name = "88.9 Noticias",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHMFM.mp3",
                logoResId = R.drawable.logo_noticias_889,
                genre = "Noticias",
                country = "México"
            ),
            RadioStation(
                id = 217,
                name = "La Z 107.3",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XEQR_FMAAC.aac",
                logoResId = R.drawable.logo_la_z,
                genre = "Regional Mexicano",
                country = "México"
            ),
            // Imagen Radio - Stream returns 404
            /*
            RadioStation(
                id = 218,
                name = "Imagen Radio",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XEDA_FMAAC.aac",
                logoResId = R.drawable.logo_imagen,
                genre = "Noticias/Voz",
                country = "México"
            ),
            */
            RadioStation(
                id = 219,
                name = "Radio Educación",
                streamUrl = "https://s2.mexside.net/8172/stream",
                logoResId = R.drawable.logo_educacion,
                genre = "Cultura",
                country = "México"
            ),
            RadioStation(
                id = 220,
                name = "Ibero 90.9",
                streamUrl = "https://shaincast.caster.fm:20866/listen.mp3",
                logoResId = R.drawable.logo_ibero,
                genre = "Alternativo",
                country = "México"
            ),
            // Fixed: Opus uses mexside.net stream
            RadioStation(
                id = 221,
                name = "Opus 94.5",
                streamUrl = "https://s2.mexside.net/8176/stream",
                logoResId = R.drawable.logo_opus,
                genre = "Música Clásica",
                country = "México"
            ),
            // New regional and additional stations
            RadioStation(
                id = 223,
                name = "Radio Disney",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHRCFMAAC.aac",
                logoResId = R.drawable.ic_radio,
                genre = "Infantil/Pop",
                country = "México"
            ),
            RadioStation(
                id = 224,
                name = "Los 40 Guadalajara",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_GDLAAC.aac",
                logoResId = R.drawable.logo_los40_mx,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 225,
                name = "Los 40 Monterrey",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_MTYAAC.aac",
                logoResId = R.drawable.logo_los40_mx,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 226,
                name = "Horizonte 107.9",
                streamUrl = "https://s2.mexside.net/8174/stream",
                logoResId = R.drawable.ic_radio,
                genre = "Jazz/World",
                country = "México"
            ),
            // Additional regional MVS and Globo stations
            RadioStation(
                id = 227,
                name = "Stereorey",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/STEREOREYAAC.aac",
                logoResId = R.drawable.ic_radio,
                genre = "Baladas/Pop",
                country = "México"
            ),
            RadioStation(
                id = 228,
                name = "Globo FM Tijuana",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHOCLAAC.aac",
                logoResId = R.drawable.ic_radio,
                genre = "Variada",
                country = "México"
            ),
            RadioStation(
                id = 229,
                name = "Globo FM Mexicali",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHPFFMAAC.aac",
                logoResId = R.drawable.ic_radio,
                genre = "Variada",
                country = "México"
            ),
            RadioStation(
                id = 230,
                name = "Globo FM Monterrey",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHJMFMAAC.aac",
                logoResId = R.drawable.ic_radio,
                genre = "Variada",
                country = "México"
            ),
            RadioStation(
                id = 231,
                name = "Exa FM Piedras Negras",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHPNSFMAAC.aac",
                logoResId = R.drawable.logo_exa_fm,
                genre = "Música",
                country = "México"
            ),
            RadioStation(
                id = 232,
                name = "La Mejor Tijuana",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/XHTIMAAC.aac",
                logoResId = R.drawable.logo_la_mejor,
                genre = "Grupera",
                country = "México"
            )
            // El Fonógrafo, Reactor, Formula, Imagen, W Radio - No public streams available
        )
    }
}
