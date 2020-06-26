import org.jsoup.Jsoup

val documents: Seq[String] =
  "https://www.nepremicnine.net/oglasi-prodaja/medulin-stanovanje_6347567/" ::
    "https://www.nepremicnine.net/oglasi-prodaja/sezana-garaza_6347637/" ::
    "https://www.nepremicnine.net/oglasi-prodaja/sestdobe-posest_6347670/" ::
    Nil



Jsoup.connect(documents.head).get()