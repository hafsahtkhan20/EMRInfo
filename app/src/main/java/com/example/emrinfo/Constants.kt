package com.example.emrinfo

import java.util.*

const val jwtIssuer = "e30dfacf-2e30-401c-ac53-54245b776f13"
const val jwtSubject = "e30dfacf-2e30-401c-ac53-54245b776f13"
const val jwtAudience = "https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token"
const val jwtId = "f93aafba-2ed9-113a-8882-5ce0c5ae3679"
val expirationTime = Date(Date().time + 240000) // Expiration Time: Current time + 4 minutes

const val tokenEndpoint = "https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token"
const val aud = "https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4"
val clientId = "e30dfacf-2e30-401c-ac53-54245b776f13"