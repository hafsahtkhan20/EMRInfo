package com.example.emrinfo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class MedicalHistory : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_history)

        var token = MyApplication.token
        var jwt = MyApplication.jwt

        accessTokenRequest(jwt)

    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun accessTokenRequest(jwt: String) {

        val formBody: RequestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            .add("client_assertion", jwt)
            .build()

        val request: Request = Request.Builder()
            .url(tokenEndpoint)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val accessTokenBody: String = response.body?.string() ?: "no response"
                    Log.d("Hafsah", "AccessTokenBody : $accessTokenBody")
                    val json = JSONObject(accessTokenBody)
                    val token = json.getString("access_token")
                    //MyApplication.token = token
                    GlobalScope.launch(Dispatchers.IO) {
                        searchPatientById(
                            token,
                            MyApplication.patientid
                        )
                    }
                }
                else {
                    Log.d("Hafsah", "Error Access Token Response ${response.message}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d("Hafsah", "Access Token Response onFailure")
            }
        })
    }

    @SuppressLint("SetTextI18n")
    @Suppress("SameParameterValue")
    private fun searchPatientById(
        token: String, id: String
    ) {
        /*runOnUiThread{patientID.text = "Patient ID: Searching Patient..."

            raceText.setText("...")
            preferredText.setText("...")
            patientSex.setText("...")
            patientPhone.setText("...")
            patientAddress.setText("...")
            patientCity.setText("...")
            patientState.setText("...")
            patientZIP.setText("...")}
*/
        val request = Request.Builder()
            .url("https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4/Observation/$id")
            .header("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val searchPatientResponse: String = response.body?.string() ?: "no response"
                    Log.d("Hafsah", "searchPatientResponse : $${searchPatientResponse}")
                    //extractPatientInfoFromXml(searchPatientResponse)
                }
                else {
                    Log.d("Hafsah", "error search patient response ${response.message}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d("Hafsah", "Search Patient onFailure : ${e.message}")
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun extractPatientInfoFromXml(xml: String) {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.parse(InputSource(StringReader(xml)))
        doc.documentElement.normalize()

        val entryList = doc.getElementsByTagName("entry")
        val entry = entryList.item(0) as Element
        val fullUrlElement = entry.getElementsByTagName("fullUrl").item(0) as Element
        val fullUrl = fullUrlElement.getAttribute("value")
        val regex = Regex("""/([^/]+)/?$""")
        val matchResult = regex.find(fullUrl)
        val lastIndex = matchResult?.groupValues?.lastOrNull()

        val fullResource = entry.getElementsByTagName("resource").item(0) as Element
        val fullRace = fullResource.getElementsByTagName("display").item(0) as Element
        val dRace = fullRace.getAttribute("value")

        val legalSex = fullResource.getElementsByTagName("display").item(1) as Element
        val pLeg = legalSex.getAttribute("value")

        val birthSex = fullResource.getElementsByTagName("display").item(2) as Element
        val pSex = birthSex.getAttribute("value")

        val homePhone = fullResource.getElementsByTagName("telecom").item(0) as Element
        val phoneVal = homePhone.getElementsByTagName("value").item(0) as Element
        val pPhone = phoneVal.getAttribute("value")

        val fullAddress = fullResource.getElementsByTagName("address").item(0) as Element

        val lineAddress = fullAddress.getElementsByTagName("line").item(0) as Element
        val patAddress = lineAddress.getAttribute("value")

        val fullCity = fullAddress.getElementsByTagName("city").item(0) as Element
        val patCity = fullCity.getAttribute("value")

        val fullState = fullAddress.getElementsByTagName("state").item(0) as Element
        val patState = fullState.getAttribute("value")

        val fullZIP = fullAddress.getElementsByTagName("postalCode").item(0) as Element
        val patZIP = fullZIP.getAttribute("value")
/*
        runOnUiThread {
            patientID.text = if (lastIndex.isNullOrEmpty()) "..." else "Patient ID : $lastIndex"

            raceText.setText(if (dRace.isNullOrEmpty()) "..." else "$dRace")
            preferredText.setText(if (pLeg.isNullOrEmpty()) "..." else "$pLeg")
            patientSex.setText(if (pSex.isNullOrEmpty()) "..." else "$pSex")
            patientPhone.setText(if (pPhone.isNullOrEmpty()) "..." else "$pPhone")
            patientAddress.setText(if (patAddress.isNullOrEmpty()) "..." else "$patAddress")
            patientCity.setText(if (patCity.isNullOrEmpty()) "..." else "$patCity")
            patientState.setText(if (patState.isNullOrEmpty()) "..." else "$patState")
            patientZIP.setText(if (patZIP.isNullOrEmpty()) "..." else "$patZIP")

        } */
    }
}