/**
 * Wallet module SPI (Service Provider Interface).
 * Exposes wallet crediting operations to other modules (e.g. revenue payout on order capture)
 * while keeping persistence and internal wiring encapsulated.
 */
@org.springframework.modulith.NamedInterface("wallet-spi")
package apps.sarafrika.elimika.wallet.service;
