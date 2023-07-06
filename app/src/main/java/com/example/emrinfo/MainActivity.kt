package com.example.emrinfo

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
import java.io.InputStreamReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private lateinit var patientFirst: EditText
    private lateinit var patientLast: EditText
    private lateinit var patientDOB: EditText
    private lateinit var patientID: TextView
    private lateinit var findButton: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        patientFirst = findViewById(R.id.pFirst)
        patientLast = findViewById(R.id.pLast)
        patientDOB = findViewById(R.id.pDOB)
        patientID = findViewById(R.id.patientID)
        findButton = findViewById(R.id.patientButton)

        findButton.setOnClickListener {
            if (patientFirst.text.toString().isEmpty() &&
                    patientLast.text.toString().isEmpty() &&
                    patientDOB.text.toString().isEmpty())
            {
                Toast.makeText(this, "Fields can't be empty", Toast.LENGTH_SHORT).show()
            }
            else {
                val privateKey = readPEMPrivateFile()

                if (privateKey != null) {
                    val jwt = Jwts.builder()
                        .setHeaderParam("alg", "RS384")
                        .setHeaderParam("typ", "JWT")
                        .setIssuer(jwtIssuer)
                        .setSubject(jwtSubject)
                        .setAudience(jwtAudience)
                        .setId(jwtId)
                        .setExpiration(expirationTime)
                        .signWith(SignatureAlgorithm.RS384, privateKey)
                        .compact()

                    GlobalScope.launch(Dispatchers.IO) {
                        accessTokenRequest(jwt)
                    }

                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readPEMPrivateFile(): PrivateKey? {
        try {
            val inputStream = applicationContext.assets.open("privatekey.pem")
            val reader = InputStreamReader(inputStream)
            val pemContent = reader.readText()
            reader.close()

            val privateKeyReplace = pemContent.replace("\\n".toRegex(), "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")

            val privateKeyBytes = Base64.getDecoder().decode(privateKeyReplace.toByteArray())
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(keySpec)
        }
        catch (ex: Exception){
            Log.d("Hafsah", "Exception: reading PEM private ${ex.message}")
        }
        return null
    }

    private fun accessTokenRequest(jwt: String) {
        runOnUiThread { patientID.text = "Patient ID: Fetching Access Token..."}

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
                    GlobalScope.launch(Dispatchers.IO) {
                        searchPatientByNameAndDOB(
                            token,
                            patientLast.text.toString().lowercase().trimIndent(),
                            patientFirst.text.toString().lowercase().trimIndent(),
                            patientDOB.text.toString().trimIndent()
                        )
                    }
                }
                else {
                    Log.d("Hafsah", "Error Access Token Response ${response.message}")
                    patientID.text = ""
                }
            }

            override fun onFailure(call:Call, e: IOException) {
                Log.d("Hafsah", "Access Token Response onFailure")
            }
        })
    }


    private fun searchPatientByNameAndDOB(
        token: String, lastName: String, firstName: String, dob: String
    ) {
        runOnUiThread{patientID.text = "Patient ID: Searching Patient..."}

        val request = Request.Builder()
            .url("https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4/Patient?family=$lastName&given=$firstName&birthdate=$dob")
            .header("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val searchPatientResponse: String = response.body?.string() ?: "no response"
                    Log.d("Hafsah", "searchPatientResponse : $${searchPatientResponse}")
                    extractPatientInfoFromXml(searchPatientResponse)
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

        runOnUiThread {
            patientID.text = if (lastIndex.isNullOrEmpty()) "..." else "Patient ID : $lastIndex"
        }
    }
}