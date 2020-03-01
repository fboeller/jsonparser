package io.fboeller;

import static io.fboeller.ParserDSLKt.*;

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
        var personOf = fields(
                mandatory(string().field("firstName")),
                string().field("lastName")
        ).mapTo(Person::new);
    }

}
