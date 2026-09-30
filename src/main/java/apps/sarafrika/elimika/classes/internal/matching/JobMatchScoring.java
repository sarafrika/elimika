package apps.sarafrika.elimika.classes.internal.matching;

import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The rules-v1 fit score between an instructor and a marketplace job (roadmap section 10). Pure
 * arithmetic over facts already loaded; it never decides eligibility, which stays with the job service.
 *
 * <pre>
 * score = 0.35·skill_coverage + 0.20·location_fit + 0.15·rate_fit + 0.10·experience + 0.10·rating + 0.10·urgency
 *         × 0.3 when a mandatory skill is missing, + 0.1 for the job's preferred instructor, clipped to 0..1
 * </pre>
 */
public final class JobMatchScoring {

    public static final String MODEL_VERSION = "rules-v1";

    static final double W_SKILLS = 0.35;
    static final double W_LOCATION = 0.20;
    static final double W_RATE = 0.15;
    static final double W_EXPERIENCE = 0.10;
    static final double W_RATING = 0.10;
    static final double W_URGENCY = 0.10;
    static final double MISSING_MANDATORY_FACTOR = 0.3;
    static final double PREFERRED_BONUS = 0.1;
    /** Location fit when distance cannot be used (instructor not opted in, or no job point). */
    static final double NEUTRAL_LOCATION = 0.5;
    /** Rating fit before the platform has any reviews. */
    static final double NEUTRAL_RATING = 0.5;
    /** Urgency of a job whose registration has no closing date. */
    static final double NO_DEADLINE_URGENCY = 0.25;
    static final int URGENT_DAYS = 3;
    static final int RELAXED_DAYS = 30;

    private JobMatchScoring() {
    }

    /** Everything the score is computed from. {@code distanceBand} is null when distance is not used. */
    public record Inputs(List<JobRequiredSkills.Tag> requiredSkills,
                         Map<UUID, ProficiencyLevel> instructorSkills,
                         LocationType locationType,
                         String distanceBand,
                         BigDecimal pay,
                         BigDecimal approvedRate,
                         double yearsOfExperience,
                         Double ratingBayes,
                         LocalDate registrationCloses,
                         LocalDate today,
                         boolean preferredInstructor) {
    }

    public record Result(double score,
                         List<UUID> matchedSkillUuids,
                         int requiredSkillCount,
                         boolean missingMandatory,
                         double skillCoverage,
                         double locationFit,
                         double rateFit,
                         double experience,
                         double rating,
                         double urgency,
                         Integer payAbovePercent) {
    }

    public static Result score(Inputs in) {
        List<JobRequiredSkills.Tag> required = in.requiredSkills() == null ? List.of() : in.requiredSkills();
        Map<UUID, ProficiencyLevel> held = in.instructorSkills() == null ? Map.of() : in.instructorSkills();
        List<UUID> matched = required.stream()
                .filter(tag -> meets(held.get(tag.skillUuid()), tag.minProficiency()))
                .map(JobRequiredSkills.Tag::skillUuid)
                .toList();
        boolean missingMandatory = required.stream()
                .anyMatch(tag -> tag.mandatory() && !meets(held.get(tag.skillUuid()), tag.minProficiency()));
        double skillCoverage = required.isEmpty() ? 1.0 : (double) matched.size() / required.size();
        double locationFit = locationFit(in.locationType(), in.distanceBand());
        double rateFit = rateFit(in.pay(), in.approvedRate());
        double experience = Math.min(Math.max(in.yearsOfExperience(), 0d) / 5d, 1d);
        double rating = in.ratingBayes() == null ? NEUTRAL_RATING : clip(in.ratingBayes() / 5d, 0, 1);
        double urgency = urgency(in.registrationCloses(), in.today());

        double score = W_SKILLS * skillCoverage + W_LOCATION * locationFit + W_RATE * rateFit
                + W_EXPERIENCE * experience + W_RATING * rating + W_URGENCY * urgency;
        if (missingMandatory) {
            score *= MISSING_MANDATORY_FACTOR;
        }
        if (in.preferredInstructor()) {
            score += PREFERRED_BONUS;
        }
        score = Math.round(clip(score, 0, 1) * 10_000d) / 10_000d;
        return new Result(score, matched, required.size(), missingMandatory, skillCoverage, locationFit, rateFit,
                experience, rating, urgency, payAbovePercent(in.pay(), in.approvedRate()));
    }

    /** An unrated or unlinked skill never meets a requirement; a held level meets any lower minimum. */
    static boolean meets(ProficiencyLevel held, ProficiencyLevel minimum) {
        if (held == null) {
            return false;
        }
        return minimum == null || held.ordinal() >= minimum.ordinal();
    }

    /** ONLINE is always 1.0; otherwise by distance band, and neutral without one. */
    static double locationFit(LocationType locationType, String distanceBand) {
        if (locationType == LocationType.ONLINE) {
            return 1.0;
        }
        if (distanceBand == null) {
            return NEUTRAL_LOCATION;
        }
        return switch (distanceBand) {
            case "<2 km" -> 1.0;
            case "2-5 km" -> 0.9;
            case "5-10 km" -> 0.75;
            case "10-25 km" -> 0.5;
            default -> 0.2;
        };
    }

    /** {@code clip((pay - rate) / rate, 0, 0.5) × 2}; 0 without both. */
    static double rateFit(BigDecimal pay, BigDecimal approvedRate) {
        if (pay == null || approvedRate == null || approvedRate.signum() <= 0) {
            return 0;
        }
        double margin = pay.subtract(approvedRate).divide(approvedRate, MathContext.DECIMAL64).doubleValue();
        return clip(margin, 0, 0.5) * 2;
    }

    /** Whole percent the pay is above the approved rate, or null when it is not above it. */
    static Integer payAbovePercent(BigDecimal pay, BigDecimal approvedRate) {
        if (pay == null || approvedRate == null || approvedRate.signum() <= 0 || pay.compareTo(approvedRate) <= 0) {
            return null;
        }
        int percent = (int) Math.round(pay.subtract(approvedRate)
                .divide(approvedRate, MathContext.DECIMAL64).doubleValue() * 100);
        return percent > 0 ? percent : null;
    }

    /** 1.0 within {@value #URGENT_DAYS} days of closing, falling linearly to 0 at {@value #RELAXED_DAYS}. */
    static double urgency(LocalDate registrationCloses, LocalDate today) {
        if (registrationCloses == null || today == null) {
            return NO_DEADLINE_URGENCY;
        }
        long days = ChronoUnit.DAYS.between(today, registrationCloses);
        if (days <= URGENT_DAYS) {
            return days < 0 ? 0 : 1.0;
        }
        if (days >= RELAXED_DAYS) {
            return 0;
        }
        return 1.0 - (double) (days - URGENT_DAYS) / (RELAXED_DAYS - URGENT_DAYS);
    }

    /**
     * The band between an opted-in instructor's point and a job's point, both already rounded; null when
     * either is missing (never a distance in metres).
     */
    public static String distanceBand(SearchGeoPoint instructorPoint, SearchGeoPoint jobPoint) {
        if (instructorPoint == null || jobPoint == null) {
            return null;
        }
        return new NearMe(instructorPoint.lat(), instructorPoint.lng(), NearMe.MIN_RADIUS_KM).distanceBand(jobPoint);
    }

    /** Reader-facing wording of a band: "Under 2 km away", "About 2-5 km away", "Over 25 km away". */
    public static String distanceReason(String band) {
        if (band == null) {
            return null;
        }
        if (band.startsWith("<")) {
            return "Under " + band.substring(1) + " away";
        }
        if (band.startsWith(">")) {
            return "Over " + band.substring(1) + " away";
        }
        return "About " + band + " away";
    }

    private static double clip(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
