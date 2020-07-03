package com.pinkstack.realestate

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.ConfigSource

case class Seed(initialCategories: List[String])

case class Pagination(categoryPagesLimit: Int)

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