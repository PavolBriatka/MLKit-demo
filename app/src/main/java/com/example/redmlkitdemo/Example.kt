package com.example.redmlkitdemo

class Example {

     enum class AbbreviationOfDays constructor(var abbreviation: String) {
        SUNDAY("SUN"), MONDAY("MON"), TUESDAY("TUES"), WEDNESDAY("WED"),
        THURSDAY("THURS"), FRIDAY("FRI"), SATURDAY("SAT")
    }

     fun whatevs() {
        AbbreviationOfDays.MONDAY.abbreviation = "LOG"
    }
}
