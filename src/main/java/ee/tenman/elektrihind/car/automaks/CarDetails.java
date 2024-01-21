package ee.tenman.elektrihind.car.automaks;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
@CarDetails.ValidCarDetails
public class CarDetails {

    private static final String CATEGORY_KEY = "Kategooria tähis";
    private static final String CO2_WLTP_KEY = "CO2 (WLTP)";
    private static final String CO2_NEDC_KEY = "CO2 (NEDC)";
    private static final String FUEL_TYPE_KEY = "Kütus";
    private static final String ENGINE_KEY = "Mootor";
    private static final String ENGINE_POWER_KEY = "Mootori võimsus";
    private static final String FULL_MASS_KEY = "Täismass";
    private static final String REGISTRATION_DATE_KEY = "Esmane registreerimine";
    private static final String ELECTRIC_FUEL_TYPE = "Elekter";
    private Double co2Emissions;
    @NotNull
    private Integer fullMass;
    @NotNull
    private Integer year;
    private Integer engineCapacity;
    private Integer enginePower;
    @NotNull
    private CarType carType;
    private boolean electric;
    private CO2Type co2Type;
    public CarDetails(Map<String, String> carDataMap) {
        this.carType = extractCarType(carDataMap.get(CATEGORY_KEY));
        setCO2Emissions(carDataMap);
        this.electric = ELECTRIC_FUEL_TYPE.equalsIgnoreCase(carDataMap.get(FUEL_TYPE_KEY));
        this.engineCapacity = Optional.ofNullable(carDataMap.get(ENGINE_KEY))
                .map(this::parseInteger)
                .orElse(null);
        this.enginePower = Optional.ofNullable(carDataMap.get(ENGINE_POWER_KEY))
                .map(this::parseInteger)
                .orElse(null);
        this.fullMass = Optional.ofNullable(carDataMap.get(FULL_MASS_KEY))
                .map(this::parseInteger)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse full mass from " + carDataMap.get(FULL_MASS_KEY)));
        this.year = Optional.ofNullable(carDataMap.get(REGISTRATION_DATE_KEY))
                .map(this::extractYear)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse year from " + carDataMap.get(REGISTRATION_DATE_KEY)));
    }

    private CarType extractCarType(String category) {
        return Optional.ofNullable(category)
                .map(CarType::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse car type from " + category));
    }

    private void setCO2Emissions(Map<String, String> carDataMap) {
        Optional<Double> co2Emissions = Optional.ofNullable(carDataMap.get(CO2_WLTP_KEY))
                .or(() -> Optional.ofNullable(carDataMap.get(CO2_NEDC_KEY)))
                .map(this::extractNumericValue)
                .map(Double::parseDouble);

        co2Emissions.ifPresent(v -> {
            this.co2Emissions = v;
            this.co2Type = carDataMap.containsKey(CO2_WLTP_KEY) ? CO2Type.WLTP : CO2Type.NEDC;
        });
    }

    private Integer parseInteger(String value) {
        return Optional.ofNullable(value)
                .map(this::extractNumericValue)
                .map(Integer::parseInt)
                .orElse(null);
    }

    private Integer extractYear(String registrationDate) {
        return Optional.ofNullable(registrationDate)
                .map(date -> date.split("\\."))
                .stream()
                .flatMap(Arrays::stream)
                .filter(s -> s.matches("\\d{4}"))
                .findFirst()
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalArgumentException("Could not extract year from " + registrationDate));
    }

    private String extractNumericValue(String string) {
        if (string == null || !string.contains(" ")) {
            return null;
        }
        
        Pattern pattern = Pattern.compile("\\b\\d{1,4}\\b");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    public enum CarType {
        M1,
        M1G,
        N1,
        N1G,
        L3e,
        L4e,
        L5e,
        L6e,
        L7e,
        MS2,
        T1b,
        T3,
        T5
    }

    public enum CO2Type {
        WLTP,
        NEDC
    }

    @Documented
    @Constraint(validatedBy = CarDetailsValidator.class)
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidCarDetails {
        String message() default "Invalid car details";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Slf4j
    public static class CarDetailsValidator implements ConstraintValidator<ValidCarDetails, CarDetails> {

        @Override
        public boolean isValid(CarDetails carDetails, ConstraintValidatorContext context) {
            boolean valid = true;
            if (carDetails.getCo2Emissions() == null && !carDetails.isElectric()) {
                if (carDetails.getEngineCapacity() == null || carDetails.getEnginePower() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Missing engineCapacity or/and enginePower for non-electric vehicle.")
                            .addPropertyNode("engineCapacity")
                            .addConstraintViolation();
                    valid = false;
                }
            } else if (carDetails.getCo2Emissions() != null && carDetails.getCo2Type() == null && !carDetails.isElectric()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Missing CO2Type for vehicle with co2Emissions.")
                        .addPropertyNode("co2Type")
                        .addConstraintViolation();
                valid = false;
            }

            return valid;
        }
    }

}
