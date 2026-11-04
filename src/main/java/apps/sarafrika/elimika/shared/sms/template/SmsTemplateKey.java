package apps.sarafrika.elimika.shared.sms.template;

import java.util.List;

/**
 * Every SMS the platform sends, with its built-in wording and the placeholders it may use.
 * Admins can reword one (stored in {@code sms_templates}); the default here is what is sent otherwise.
 */
public enum SmsTemplateKey {

    PAYMENT_RECEIVED("Payment received", "Payments", false,
            "Sent once to the number that paid, the moment a payment is captured. The receipt email goes at the same time.",
            "Elimika: Paid {amount} for {class}. Order {order}, M-Pesa {mpesa}. First class {first_session}. See you there!",
            List.of(Placeholder.AMOUNT.required(), Placeholder.CLASS, Placeholder.ORDER.required(), Placeholder.MPESA,
                    Placeholder.FIRST_SESSION)),

    SEAT_RELEASED("Seat released", "Payments", false,
            "Sent once to the number the M-Pesa prompt went to, when the 5-minute seat hold runs out unpaid. Never sent if no prompt went out.",
            "Elimika: Order {order} was not paid, so your seat in {class} was released. Nothing was charged.",
            List.of(Placeholder.ORDER.required(), Placeholder.CLASS)),

    WITHDRAWAL_CODE("Withdrawal code", "Wallet security", true,
            "Sent to the payout number with the code that confirms a withdrawal. Cannot be switched off; must include {code}.",
            "Your Elimika code to withdraw {amount} to this number is {code}. It expires in {expires} minutes. Never share it with anyone.",
            List.of(Placeholder.CODE.required(), Placeholder.AMOUNT, Placeholder.EXPIRES)),

    PAYOUT_NUMBER_CODE("Payout number code", "Wallet security", true,
            "Sent to a new payout number to prove it belongs to the earner. Cannot be switched off; must include {code}.",
            "Your Elimika code to add this number for withdrawals is {code}. It expires in {expires} minutes. Never share it with anyone.",
            List.of(Placeholder.CODE.required(), Placeholder.EXPIRES));

    private final String title;
    private final String group;
    private final boolean security;
    private final String trigger;
    private final String defaultBody;
    private final List<Placeholder> placeholders;

    SmsTemplateKey(String title, String group, boolean security, String trigger, String defaultBody, List<Placeholder> placeholders) {
        this.title = title;
        this.group = group;
        this.security = security;
        this.trigger = trigger;
        this.defaultBody = defaultBody;
        this.placeholders = placeholders;
    }

    public String title() { return title; }
    public String group() { return group; }
    /** Security messages carry a one-time code: always sent, and the code placeholder cannot be dropped. */
    public boolean security() { return security; }
    public String trigger() { return trigger; }
    public String defaultBody() { return defaultBody; }
    public List<Placeholder> placeholders() { return placeholders; }

    /** A value filled into a template at send time, with a sample and the longest it can be. */
    public record Placeholder(String name, String label, String sample, int maxLength, boolean isRequired) {
        static final Placeholder AMOUNT = new Placeholder("amount", "Amount", "KES 10,500", 11, false);
        static final Placeholder CLASS = new Placeholder("class", "Class title (cut to 32)", "Flower Gardening - Sat cohort", 32, false);
        static final Placeholder ORDER = new Placeholder("order", "Order number", "ELM-HH48B3", 10, false);
        static final Placeholder MPESA = new Placeholder("mpesa", "M-Pesa receipt", "UK42C120ZR", 10, false);
        static final Placeholder FIRST_SESSION = new Placeholder("first_session", "First session, EAT", "Sat 14 Nov 09:00", 16, false);
        static final Placeholder CODE = new Placeholder("code", "One-time code", "482913", 6, false);
        static final Placeholder EXPIRES = new Placeholder("expires", "Minutes until the code expires", "10", 2, false);

        Placeholder required() {
            return new Placeholder(name, label, sample, maxLength, true);
        }
    }
}
