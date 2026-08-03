/**
 * Read-only payout lookup SPI exposed by the shared module.
 * <p>
 * Lets a module read what an organisation owes its instructors without depending on the payout
 * module directly, keeping the obligation dependency one-way: classes cause obligations, payout
 * records them.
 */
package apps.sarafrika.elimika.shared.spi.payout;
