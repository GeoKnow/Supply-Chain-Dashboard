package supplychain.simulator

import supplychain.model.Product

/**
 * Defines a simulation, i.e., its parameters.
 */
trait Simulation {
  def product: Product
}

/**
 * Simple chair example simulation.
 */
object ChairSimulation extends Simulation {

  val product =
    Product(
      name = "Chair",
      parts =
        Product(
          name = "Legs",
          count = 4
        ) ::
        Product(
          name = "Back",
          parts = Product("Side_Rails", 2) :: Product("Back_Support", 1) :: Nil
        ) :: Nil
    )

}

/**
 * Car simulation.
 * Currently incomplete.
 */
object CarSimulation extends Simulation {

  private def doors =
    Product(
      name = "Door",
      count = 4,
      parts = Product("Door_Handle") ::
              Product("Door_Seal", parts = Product("Door_Watershield") :: Product("Hinge") :: Nil) ::
              Product("Door_Latch") :: Nil
    )

  private def windows =
    Product(
      name = "FrontShield",
      count = 1,
      parts = Product("Front_Glass") :: Product("FrontWindowSeal") :: Nil
    ) ::
    Product(
      name = "RearShield",
      count = 1,
      parts = Product("Rear_Glass") :: Product("RearWindowSeal") :: Nil
    ) :: Nil

  val product =
    Product(
      name = "Car",
      parts = doors :: windows
    )

}

object XybermotiveDemoSimulation extends Simulation {
  private def zulieferer =
    Product(
      name = "PLS-Zulieferer",
      count = 4,
      parts = unterzulieferer :: Product("Allgaier-Lohnfertiger") :: Nil
    )

  private def unterzulieferer =
    Product(
      name = "OpticBalzer-Unterzulieferer",
      count = 16,
      parts = Nil
    )

  val product =
    Product(
      name = "Getrag-OEM",
      parts = zulieferer :: Nil
    )
}

/**
 * Fair Phone simulation.
 * Parts List based on https://www.fairphone.com/wp-content/uploads/2013/04/20130910_List-of-Suppliers.pdf

##Software

Fairphone OS Kwame Corp (Portugal)
Software optimization A'Hong (China, Chongqing)


## Hardware (Provided by A'Hong; Last update: September 2013)

Manufacturer/Developer A'Hong (China)
Tantalum Capacitor AVX (USA)
Tin Soldering Paste Alpha/Alent (USA)
Charger Salcomp (Finland)


### 2nd Tier

Screen  Success Electronics Ltd. (China)
Touchpad Success Electronics Ltd. (China)
Panel Glass (DragontrailTM) Asahi Glass Co. (Japan)
SoC (chipset) Mediatek (Taiwan)
Primary Camera Sunny Optical (China)
Secondary Camera Q-Tech Semiconductors Limited (China)
Wireless Modules (Wi-Fi, Bluetooth, GPS, etc) Azurewave (China)
Battery Fenghua-lib. Company (China)
E-compass Memsic (USA)
Accelerometer (G-sensor) Bosch (Germany)
Gyroscope InvenSense Technology (USA)
Back metallic battery cover Meng Zhifang Corporation (China)
Housing material (recycled polycarbonate) Samsung Cheil Industries (Korea)
Printed Circuit Board China Circuit Technology Corporation (China)

 * Currently incomplete.
 */
object FairPhoneSimulation extends Simulation {

  private def software = Product(
    name = "Fairphone_OS",
    count = 1,
    parts = Product("Software_optimization") :: Nil
  )

  private def hardware = Product(
    name = "Hardware_Manufacturer",
    count = 1,
    parts =
      Product("Tantulum_Capacitor", count = 10) ::
      Product("Tin_Soldering_Paste") ::
      Product("Charger") ::
      Product("Screen") ::
      Product("Touchpad") ::
      Product("Panel_Glass") ::
      Product("Primary_Camera") ::
      Product("Secondary_Camera") ::
      Product("Wireless_Modules") ::
      Product("Battery") ::
      Product("Housing_material") ::
      Product("Printed_Circuit_Board") ::
      Nil
  )

  val product =
    Product(
      name = "FairPhone",
      parts = software :: hardware :: Nil
    )

}
