package amosalexa.handlers;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

/**
 * Service annotation to provide help.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    FunctionGroup functionGroup();

    String functionName();

    String example();

    String description();

    enum FunctionGroup {
        PRODUCT_QUERY("Produktanfrage"),
        ACCOUNT_INFORMATION("Kontoinformation"),
        SMART_FINANCING("Smart Financing"),
        ONLINE_BANKING("Online Banking"),
        BUDGET_TRACKING("Budget"),
        BANK_CONTACT("Bank Kontakt");

        /**
         * Feature that belongs to a category.
         */
        public static class Feature {
            public String name;
            public String description;
            public String example;

            public Feature(String name, String description, String example) {
                this.name = name;
                this.description = description;
                this.example = example;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        public String name;
        public List<FunctionGroup.Feature> features = new LinkedList<>();

        FunctionGroup(String name) {
            this.name = name;
        }

        public void addFeature(FunctionGroup.Feature feature) {
            features.add(feature);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
