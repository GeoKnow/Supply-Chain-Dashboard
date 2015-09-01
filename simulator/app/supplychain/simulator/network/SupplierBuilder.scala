package supplychain.simulator.network

import supplychain.dataset.{WeatherProvider, ConfigurationProvider}
import supplychain.model.{Product, Supplier}

class SupplierBuilder(wp: WeatherProvider, cp: ConfigurationProvider) {

  def apply(product: Product): Seq[Supplier] = {
    for (part <- product :: product.partList) yield cp.getSupplier(part)
  }
}
