package com.bitbuzz.hydrawatch
import android.annotation.SuppressLint
import android.content.Context
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        const val SSID = "hydra"; const val PASS = "notforfun"
        const val BASE = "http://192.168.4.1"
        const val EP_START = "$BASE/run-attack"; const val EP_STOP = "$BASE/reset"
    }
    private var pending = ""; private lateinit var http: OkHttpClient
    private lateinit var sHome:View; private lateinit var sMenu:View
    private lateinit var sConsent:View; private lateinit var sAttack:View
    private lateinit var tvStatus:TextView; private lateinit var tvConsentAtk:TextView
    private lateinit var tvAtkName:TextView; private lateinit var tvLog:TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        sHome=v(R.id.sHome); sMenu=v(R.id.sMenu); sConsent=v(R.id.sConsent); sAttack=v(R.id.sAttack)
        tvStatus=tv(R.id.tvStatus); tvConsentAtk=tv(R.id.tvConsentAtk); tvAtkName=tv(R.id.tvAtkName); tvLog=tv(R.id.tvLog)
        show(sHome)
        btn(R.id.btnConnect){connect()}; btn(R.id.btnAttacks){show(sMenu)}
        btn(R.id.btnBle){pending="BLE";tvConsentAtk.text="BLE SPAM";show(sConsent)}
        btn(R.id.btnBeacon){pending="BEACON";tvConsentAtk.text="BEACON FLOOD";show(sConsent)}
        btn(R.id.btnBoth){pending="BOTH";tvConsentAtk.text="BOTH [MAX]";show(sConsent)}
        btn(R.id.btnMenuBack){show(sMenu)}
        btn(R.id.btnYes){tvAtkName.text=tvConsentAtk.text;tvLog.text="> init...\n";show(sAttack);fire(pending)}
        btn(R.id.btnNo){show(sMenu)}; btn(R.id.btnStop){stop();show(sHome)}
    }
    private fun show(v:View){listOf(sHome,sMenu,sConsent,sAttack).forEach{it.visibility=View.GONE};v.visibility=View.VISIBLE}
    private fun connect(){
        tvStatus.text="> CONNECTING..."
        val spec=WifiNetworkSpecifier.Builder().setSsid(SSID).setWpa2Passphrase(PASS).build()
        val req=NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(spec).build()
        val cm=getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.requestNetwork(req,object:ConnectivityManager.NetworkCallback(){
            override fun onAvailable(net:Network){cm.bindProcessToNetwork(net);http=OkHttpClient.Builder().socketFactory(net.socketFactory).build();ui{tvStatus.text="● CONNECTED"}}
            override fun onUnavailable(){ui{tvStatus.text="✗ FAILED"}}
        })
    }
    private fun fire(type:String){
        val f=FormBody.Builder()
        when(type){"BLE"->{f.add("attack_type","9");f.add("attack_method","0")};"BEACON"->{f.add("attack_type","5");f.add("attack_method","3")};"BOTH"->{f.add("attack_type","9");f.add("attack_method","0")};else->return}
        if(!::http.isInitialized){ui{tvLog.append("> ERR: not connected\n")};return}
        http.newCall(Request.Builder().url(EP_START).post(f.build()).build()).enqueue(object:Callback{
            override fun onFailure(c:Call,e:IOException)=ui{tvLog.append("> ERR\n")}
            override fun onResponse(c:Call,r:Response)=ui{tvLog.append(if(r.isSuccessful)"> LIVE\n" else "> ERR:${r.code}\n")}
        })
    }
    private fun stop(){if(!::http.isInitialized)return;http.newCall(Request.Builder().url(EP_STOP).head().build()).enqueue(object:Callback{override fun onFailure(c:Call,e:IOException){}; override fun onResponse(c:Call,r:Response){}})}
    private fun ui(f:()->Unit)=runOnUiThread(f); private fun v(id:Int):View=findViewById(id)
    private fun tv(id:Int):TextView=findViewById(id); private fun btn(id:Int,f:()->Unit)=findViewById<Button>(id).setOnClickListener{f()}
}
