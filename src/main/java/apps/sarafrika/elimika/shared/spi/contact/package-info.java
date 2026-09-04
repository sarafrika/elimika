/**
 * Contact Relationship Service Provider Interface (SPI) - who may see whose contact details.
 * <p>
 * Declared in {@code shared} and implemented by whichever module owns the link, so the tenancy
 * module never has to reach into timetabling or course to answer the question.
 */
package apps.sarafrika.elimika.shared.spi.contact;
