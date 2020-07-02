val coords = "(45.9818666,14.2267596)"

val c =
  """(-?\d+(\.\d+)?),\s*(-?\d+(\.\d+)?)""".r.findFirstIn(coords)
    .map(_.split(",", 2).map(_.toDouble).toList)

c match {
  case Some(v) => v
  case _ => "nopu"
}

