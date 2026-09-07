package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "ClassEnrolmentEligibility",
        description = "Whether a student may join a class, decided from the records the platform already "
                + "holds rather than from anything the learner declares about themselves."
)
public record ClassEnrolmentEligibilityDTO(

        @Schema(description = "**[READ-ONLY]** True when every check below passes and the seat can be bought.")
        @JsonProperty("eligible")
        boolean eligible,

        @Schema(description = "**[READ-ONLY]** The student's age from their recorded date of birth, or null when none is on file.", nullable = true)
        @JsonProperty("student_age")
        Integer studentAge,

        @Schema(description = "**[READ-ONLY]** Minimum age the course requires, when it sets one.", nullable = true)
        @JsonProperty("minimum_age")
        Integer minimumAge,

        @Schema(description = "**[READ-ONLY]** Maximum age the course allows, when it sets one.", nullable = true)
        @JsonProperty("maximum_age")
        Integer maximumAge,

        @Schema(description = "**[READ-ONLY]** False when a date of birth is needed to judge age but none is recorded.")
        @JsonProperty("date_of_birth_on_file")
        boolean dateOfBirthOnFile,

        @Schema(description = "**[READ-ONLY]** True when the recorded age satisfies the course's limits.")
        @JsonProperty("age_requirement_met")
        boolean ageRequirementMet,

        @Schema(description = "**[READ-ONLY]** True when at least one scheduled session still has a free seat.")
        @JsonProperty("seats_available")
        boolean seatsAvailable,

        @Schema(description = "**[READ-ONLY]** True when the student already holds a seat in this class.")
        @JsonProperty("already_enrolled")
        boolean alreadyEnrolled,

        @Schema(description = "**[READ-ONLY]** True when today falls inside the class's registration window.")
        @JsonProperty("registration_open")
        boolean registrationOpen,

        @Schema(description = "**[READ-ONLY]** The dates between which this class accepts enrolments.", nullable = true)
        @JsonProperty("registration_window")
        RegistrationWindow registrationWindow,

        @Schema(description = "**[READ-ONLY]** Why the student cannot join, phrased for them to read. Null when eligible.", nullable = true)
        @JsonProperty("reason")
        String reason
) {

    /**
     * The days on which a class accepts enrolments, both ends inclusive.
     * <p>
     * These dates are published on the class listing, so naming them in a refusal tells the learner
     * nothing they could not already read.
     */
    @Schema(
            name = "ClassRegistrationWindow",
            description = "The first and last day, both inclusive, on which a class accepts enrolments."
    )
    public record RegistrationWindow(

            @Schema(description = "**[READ-ONLY]** First day enrolment is accepted.", format = "date")
            @JsonProperty("opens_on")
            LocalDate opensOn,

            @Schema(description = "**[READ-ONLY]** Last day enrolment is accepted.", format = "date")
            @JsonProperty("closes_on")
            LocalDate closesOn
    ) {
    }
}
