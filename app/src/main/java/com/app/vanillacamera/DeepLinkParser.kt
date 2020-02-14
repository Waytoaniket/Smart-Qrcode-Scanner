package com.app.vanillacamera

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonObject


object DeepLinkParser {

    private val GOOGLE_PAY_PACKAGE_NAME = "com.google.android.apps.nbu.paisa.user"
    private val GOOGLE_PAY_REQUEST_CODE = 123
    private var lastMsg = ""
    lateinit var userId: String
    var payeename = ""
    var mcc = ""
    var vpa = ""
    var AccountNo = ""
    var Ifsc = ""
    var Amount = ""
    var aadhaar = ""
    var mam = ""
    var currency = ""
    var tr = ""
    var url = ""
    var tn = ""

    var snippetShowLoader : ((showHide : Boolean) -> Unit)? = null
  private lateinit var jsonObject :JsonObject
    fun detectUrl(url:String,activity : Activity,snippet : (url:String,isUpi : Boolean) -> Unit = {url,isUpi-> })
    {

        if(url.startsWith("http"))
        {
            openWebFromQR(activity, url)

        }
        else if(url.startsWith("upi://"))
        {
            openUpiChooserFromQR(activity,url)
            snippet.invoke(url,true)
        }

        else if(url.startsWith("0002010"))
        {

            var tt = bharatQrRead(url)

            try {
                openUpiChooserFromQR(activity, tt!!)
                snippet.invoke(tt, true)
            }
            catch (e : Exception)
            {
                Log.e("BharatQR","Parse Error")
                e.printStackTrace()
            }
        }


        else if(url.startsWith("BEGIN"))
        {
            saveContactFromQR(activity, url)
        }

        else if(url.startsWith("WIFI"))
        {
            conectWifiFromQR(activity, url)
        }

        else {
            AlertDialog.Builder(activity)
                .setTitle("Text")
                .setPositiveButton("Ok"){dialog: DialogInterface?, which: Int ->
                    dialog?.dismiss()
                }
                .setMessage(url).create().show()
        }
    }


    private fun openWebFromQR(activity: Activity,url: String)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(browserIntent)
    }

    private fun openUpiChooserFromQR(activity: Activity,url: String)
    {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        //intent.setPackage(GOOGLE_PAY_PACKAGE_NAME)
        activity.startActivityForResult(intent,
            GOOGLE_PAY_REQUEST_CODE
        )


    }

    private fun saveContactFromQR(activity: Activity,url: String)
    {
        try {

            var contact = parseVcardPojo(url)

            val intent = Intent(Intent.ACTION_INSERT)
            intent.type = ContactsContract.Contacts.CONTENT_TYPE

            intent.putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, contact.mobile)
            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE,contact.mobile)
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, contact.email)
            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, contact.organisation)

            activity.startActivity(intent)

        }
        catch (e : Exception)
        {
           e.printStackTrace()
        }

    }

    private fun parseVcardPojo(rawData : String) : VCardPojo
    {
        var data = VCardPojo()
        for (item in rawData.split("\n"))
        {
            if(item.trim().startsWith("N:"))
            {
                var name = item.split("N:").last().split(";")
                var fullName = ""
                for (item in name)
                {
                    fullName += " $item"
                }
                data.name = fullName.trim()
            }
            else if(item.trim().startsWith("ORG:"))
            {
                data.organisation = item.split("ORG:").last()
            }
            else if(item.trim().startsWith("EMAIL"))
            {
                data.email = item.trim().split(":").last()
            }
            else if(item.trim().startsWith("URL:"))
            {
                data.url = item.trim().split(":").last()
            }
            else if(item.trim().contains("CELL"))
            {
                data.mobile = item.trim().split(":").last()
            }
            else if(item.trim().contains("FAX"))
            {
                data.fax = item.trim().split(":").last()
            }

            else if(item.trim().contains("ADR"))
            {
                data.addr = item.trim().split(":").last()
            }
        }

        return data
    }


    private fun conectWifiFromQR(activity: Activity,url: String)
    {
        try {

            var wifiPojo = parseWifiData(url)
            var wifimanager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifimanager.isWifiEnabled)
                wifimanager.isWifiEnabled = true

            var wifiConfig = WifiConfiguration()
            wifiConfig.SSID = wifiPojo.ssid
            wifiConfig.preSharedKey = wifiPojo.password

            var netId = wifimanager.addNetwork(wifiConfig)
            wifimanager.disconnect()
            wifimanager.enableNetwork(netId, true)
            wifimanager.reconnect()
        }
        catch (e : Exception)
        {
           e.printStackTrace()
        }

    }


    private fun parseWifiData( url: String) : WifiPojo
    {
        var wifiPojo = WifiPojo()

        for(item in url.split(";"))
        {
            if(item.startsWith("WIFI"))
                wifiPojo.ssid = item.split(":").last()
            else if(item.startsWith("T:"))
                wifiPojo.password = item.split(":").last()
        }

        return wifiPojo

    }

    fun bharatQrRead( a: String): String? {
        try {
            var i = 4

            while (i < a.length) {
                val x = Integer.parseInt(a.substring(i - 4, i - 2))
                val y = Integer.parseInt(a.substring(i - 2, i))
                i = i + y

                if (x == 8) {
                    AccountNo = a.substring(i - y + 11, i)
                    Ifsc = a.substring(i - y, i - y + 11)
                }
                if (x == 52)
                    mcc = a.substring(i - y, i)
                if (x == 53)
                    currency = a.substring(i - y, i)
                if (x == 54)
                    Amount = a.substring(i - y, i)
                if (x == 59)
                    payeename = a.substring(i - y, i)

                println("tag" + x.toString() + " " + y.toString() + " " + a.substring(i - y, i))

                if (x == 26 || x == 27 || x == 28 || x == 62) {
                    var p = i - y + 4
                    while (p < i + 2) {
                        val q = Integer.parseInt(a.substring(p - 4, p - 2))
                        val r = Integer.parseInt(a.substring(p - 2, p))
                        p = p + r

                        if (x == 26 && q == 1)
                            vpa = a.substring(p - r, p)
                        if (x == 26 && q == 2)
                            mam = a.substring(p - r, p)
                        if (x == 27 && q == 1)
                            tr = a.substring(p - r, p)
                        if (x == 27 && q == 2)
                            url = a.substring(p - r, p)
                        if (x == 28 && q == 1)
                            aadhaar = a.substring(p - r, p)
                        if (x == 62 && q == 8)
                            tn = a.substring(p - r, p)
                        println(
                            "subtag" + q.toString() + " " + r.toString() + " " + a.substring(
                                p - r,
                                p
                            )
                        )
                        p += 4
                    }
                }
                i += 4
            }
            println("Payee Name: $payeename")
            println("MCC code: $mcc")
            println("UPI ID: $vpa")
            println("Account No: $AccountNo")
            println("IFSC: $Ifsc")
            println("Aadhaar No: $aadhaar")
            println("Amount: $Amount")
            println("Minimum Amount: $mam")
            println("Currency: $currency")
            println("Txn Ref No: $tr")
            println("URL: $url")
            println("Note: $tn")

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        var uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("am", Amount)
            .appendQueryParameter("mam", mam)
            .appendQueryParameter("pn", payeename)
            .appendQueryParameter("mc", mcc)
            .appendQueryParameter("tr", tr)
            .appendQueryParameter("tn", tn)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("url", url)
            .build();

        return uri.toString()
    }

}