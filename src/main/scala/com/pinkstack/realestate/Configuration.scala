package com.pinkstack.realestate

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.ConfigSource
import pureconfig.generic.ProductHint

case class Seed(initialCategories: List[String]) {
}

case class Pagination(categoryPagesLimit: Int) {
  // TODO: There is a problem with environment variables substitution
  // Issue:
  def this(categoryPagesLimit: String) = this(categoryPagesLimit.toInt)
}

case class Fetching(categoriesParallelism: Int, estatesParallelism: Int)

case class ThrottlingEstates(elements: Int, per: Int, maximumBurst: Int)

case class Configuration(seed: Seed,
                         pagination: Pagination,
                         fetching: Fetching,
                         throttlingEstates: ThrottlingEstates)

object Configuration {
  def loadOrThrow: Configuration =
    ConfigSource.default.loadOrThrow[Configuration]
}