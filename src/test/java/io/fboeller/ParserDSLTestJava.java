package io.fboeller;

public class ParserDSLTestJava {

    private static class Person {
        private final String firstName;
        private final String lastName;

        private Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    public static void main(String[] args) {
        var personOf = ParserDSL.Companion.mapTo(
                ParserDSL.Companion.fields(
                        ParserDSL.Companion.mandatory(ParserDSL.Companion.field(ParserDSL.Companion.getString(), "firstName")),
                        ParserDSL.Companion.field(ParserDSL.Companion.getString(), "lastName")
                ),
                Person::new
        );
    }

}
