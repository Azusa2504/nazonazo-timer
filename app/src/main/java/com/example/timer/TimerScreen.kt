package com.example.timer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.SystemClock
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


fun setAlarm(context: Context, delayMillis: Long) {
    val intent = Intent(context, TimerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerTime = System.currentTimeMillis() + delayMillis

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12以上：明示的な許可が必要
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // 許可されていないので、設定画面へ誘導
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    } else {
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}




@Composable

fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val riddles = listOf(
        "きゅうきゅうしゃ の うんてんしゅさん が いっぽうつうこう の みち を はんたい に あるいていました。でも、おまわりさん は おこりません。なんで？",
        "ばなな と みかん を にだい に のせた とらっく が きゅうカーブ で なにか を おとしました。なんでしょう？",
        "てんし　が　のっている　のりもの　は　なーんだ？",
        "119ばん　つうほう　を　うけた　しょうぼうしゃ　が　こうばん　で　とまったよ。なんでかな？",
        "のるまえ　に　まずは　おりないと　いけない　のりもの　は　なーんだ？",
        "「じじじじじじじじじじ　しゃ」　これは　なんの　のりものでしょう？",
        "「てん」を　つけると　ぎゅうぎゅうづめ　に　なってしまう　のりもの　なーんだ？",
        "うえ　と　した　には　すすめるけど、まえ　と　うしろ　には　すすめない　のりもの　なーんだ？",
        "モォ〜　となく　どうぶつが　のっている　くるまは　しょうぼうしゃ　と　パトカー　の　どっち？",
        "とまっている　ときも　うごいていないと　いけない　くるまは　なーんだ？",
        "べろが　まっくろな　のりもの　なーんだ？"

    )
    var currentRiddle by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var inputText by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(1) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedCar by remember { mutableStateOf<Int?>(null) }
    var containerWidth by remember { mutableStateOf(0) }
    var riddleMode by remember { mutableStateOf(false) }
    val isArrived = timeLeft == 0 && !isRunning && selectedCar != null
    var triggerRiddle by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }
    var revealAnswer by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imageSizeDp = 64.dp
    val imageSizePx = with(density) { imageSizeDp.toPx() }
    val scrollState = rememberScrollState()

    val carOffsetX by remember(timeLeft, totalTime, isRunning, containerWidth) {
        derivedStateOf {
            if (!isRunning || totalTime == 0 || containerWidth == 0) 0.dp
            else {
                val rightMargin = 32.dp
                val progress = timeLeft.toFloat() / totalTime
                val totalDistancePx = containerWidth - imageSizePx + with(density) { 0.dp.toPx()  } // ← ここ！
                Dp(progress * totalDistancePx / density.density)
            }
        }
    }


    val arrivalMappings = if (riddleMode) {
        mapOf(
            R.drawable.bus to Pair("なぞなぞえき", R.drawable.station),
            R.drawable.tank to Pair("なぞなぞスタンド", R.drawable.stand),
            R.drawable.ambulance to Pair("なぞなぞびょういん", R.drawable.hospital),
            R.drawable.mixer to Pair("なぞなぞこうじげんば", R.drawable.construction),
            R.drawable.policecar to Pair("なぞなぞこうばん", R.drawable.koban),
            R.drawable.fireengine to Pair("なぞなぞやま", R.drawable.fire)
        )
    } else {
        mapOf(
            R.drawable.bus to Pair("なぞなぞえき", R.drawable.station),
            R.drawable.tank to Pair("なぞなぞ\nスタンド", R.drawable.stand),
            R.drawable.ambulance to Pair("なぞなぞ\nびょういん", R.drawable.hospital),
            R.drawable.mixer to Pair("なぞなぞ\nこうじげんば", R.drawable.construction),
            R.drawable.policecar to Pair("なぞなぞ\nこうばん", R.drawable.koban),
            R.drawable.fireengine to Pair("なぞなぞやま", R.drawable.fire)
        )
    }
// 起動時に SharedPreferences から endTime を取得して現在時刻と比較する
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("end_time", 0L)
        val finished = prefs.getBoolean("timer_finished", false)

        if (endTime > System.currentTimeMillis()) {
            // タイマー継続中 → 再開
            val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
            timeLeft = remaining
            totalTime = remaining
            isRunning = true
        } else if (finished) {
            // タイマー終了済み → なぞなぞモード
            riddleMode = true
            showAnswer = false
            revealAnswer = false
        } else {
            // ✅ 初回起動や状態が保存されていない場合
            selectedCar = null
            riddleMode = false
            timeLeft = 0
            totalTime = 1
        }

        // ✅ タイマー終了済みフラグはリセットしておく
        prefs.edit().putBoolean("timer_finished", false).apply()
        isInitialized = true
    }







    LaunchedEffect(triggerRiddle) {
        if (triggerRiddle) {
            delay(300L)
            currentRiddle = riddles.random()
            triggerRiddle = false
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            val endTime = prefs.getLong("end_time", 0L)

            while (isRunning) {
                val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                if (remaining <= 0) {
                    timeLeft = 0
                    isRunning = false

                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(context, R.raw.stop).apply {
                        setOnCompletionListener { it.release() }
                        start()
                    }
                    break
                } else {
                    timeLeft = remaining
                }

                delay(1000L)
            }
        }
    }

    val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)



    LaunchedEffect(showAnswer) {
        if (showAnswer) {
            delay(1000L) // 1秒後に
            revealAnswer = true
        }
    }

if(!isInitialized){
    //初期化中はローディング表示
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        CircularProgressIndicator()
    }
} else {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Thin,
                        fontStyle =
                            FontStyle.Italic,
                        color = Color(0xFF424242),

                        )
                ) {
                    append("なぞなぞ")
                }
                withStyle(
                    style = SpanStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF42A5F5), // カッコかわいいブルー
                        shadow = Shadow(
                            color = Color.Gray,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                ) {
                    append("タイマー")
                }
            },
            modifier = Modifier
                .padding(top = 24.dp, bottom = 24.dp)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )

        if (!riddleMode) {

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("なんふんにする？") },
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "すきなくるまをえらんでスタートしてね",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF0077CC),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )

            val carImages = listOf(
                R.drawable.ambulance,
                R.drawable.bus,
                R.drawable.mixer,
                R.drawable.tank,
                R.drawable.policecar,
                R.drawable.fireengine
            )

            // 車選択
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(arrivalMappings.keys.toList()) { carImage ->
                    Image(
                        painter = painterResource(id = carImage),
                        contentDescription = "Car",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(imageSizeDp)
                            .clickable {
                                val minutes = inputText.toIntOrNull()
                                if (!isRunning && minutes != null && minutes > 0) {
                                    selectedCar = carImage
                                    prefs.edit().putInt("selected_car", carImage).apply()

                                    totalTime = minutes * 60
                                    timeLeft = totalTime
                                    isRunning = true
                                    riddleMode = false  // なぞなぞモードを解除
                                    val prefs = context.getSharedPreferences(
                                        "timer_prefs",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = prefs.edit()
                                    val endTime = System.currentTimeMillis() + totalTime * 1000
                                    editor.putLong("end_time", endTime)
                                    editor.putInt("selected_car", selectedCar!!)

                                    editor.apply()
                                    // アラーム設定
                                    setAlarm(context, totalTime * 1000L)
                                }
                            }
                    )
                }
            }


            if (selectedCar != null) {
                val (arrivalLabel, arrivalImageRes) = arrivalMappings[selectedCar]!!

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp)
                ) {
                    Text(
                        text = arrivalLabel,
                        color = Color(0xFFEC407A),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 5.dp, bottom = 0.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 60.dp) // ⭐ 左右に余白を追加
                            .height(100.dp)
                            .onGloballyPositioned { coordinates ->
                                containerWidth = coordinates.size.width
                            }
                    ) {
                        // 目的地アイコン
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-40).dp)
                        ) {
                            Image(
                                painter = painterResource(id = arrivalImageRes),
                                contentDescription = arrivalLabel,
                                modifier = Modifier
                                    .size(145.dp)
                                    .padding(bottom = 10.dp)
                            )
                        }

                        // 車アイコン
                        Image(
                            painter = painterResource(id = selectedCar!!),
                            contentDescription = "Running Car",
                            modifier = Modifier
                                .offset(x = carOffsetX, y = 80.dp)
                                .size(90.dp)
                                .padding(bottom = 20.dp)
                        )
                    }


                }
            }


            Spacer(modifier = Modifier.height(55.dp))

            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            Text("のこり：${minutes}ふん${seconds}びょう")
            if (riddleMode) {
                if (currentRiddle.isNotEmpty()) {
                    Text(
                        text = currentRiddle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        modifier = Modifier.padding(0.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = {
                        triggerRiddle = true
                        showAnswer = false
                        revealAnswer = false
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp)
                ) {
                    Text("ほかのもんだい ▶", color = Color(0xFF0077CC))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🔹 タップで答えを表示するゾーン
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showAnswer = true
                        revealAnswer = false
                    }
                ) {
                    Text("はるちゃんゆうちゃんのいえ", color = Color(0xFF6A0DAD))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        listOf(R.drawable.home, R.drawable.tearai).forEach { imageRes ->
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }
            if ((isArrived || (riddleMode && selectedCar != null && currentRiddle.isEmpty()))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            mediaPlayer?.let { player ->
                                try {
                                    if (player.isPlaying) {
                                        player.stop()
                                    }
                                    player.release()
                                } catch (e: IllegalStateException) {
                                    e.printStackTrace()
                                } finally {
                                    mediaPlayer = null
                                }
                            }


                            currentRiddle = ""
                            riddleMode = true        // ← 必須！
                            triggerRiddle = true     // ← 必須！
                            showAnswer = false       // ← 任意（答え非表示）
                            revealAnswer = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEB3B),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(60.dp)
                            .width(200.dp)
                    ) {
                        Text(
                            text = "なぞなぞ",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

        }
        if (riddleMode && selectedCar != null) {
            val arrival = arrivalMappings[selectedCar]

            if (arrival != null) {
                val (arrivalLabel, arrivalImageRes) = arrival

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 到着ラベルとアイコン
                    Text(
                        text = arrivalLabel,
                        color = Color(0xFF6A0DAD),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Image(
                        painter = painterResource(id = arrivalImageRes),
                        contentDescription = "Station Icon",
                        modifier = Modifier.size(140.dp)
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    // なぞなぞ本文
                    if (currentRiddle.isNotEmpty()) {
                        Text(
                            text = currentRiddle,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // ほかの問題ボタン
                    TextButton(
                        onClick = {
                            triggerRiddle = true
                            showAnswer = false
                            revealAnswer = false
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 16.dp)
                    ) {
                        Text("ほかのもんだい ▶", color = Color(0xFF0077CC))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // はるちゃんゆうちゃんのいえエリア
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showAnswer = true
                            revealAnswer = false
                        }
                    ) {
                        Text(
                            "はるちゃんゆうちゃんのいえ",
                            color = Color(0xFF6A0DAD),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFF176))
                                .padding(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(R.drawable.home, R.drawable.tearai).forEach { imageRes ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFFFF176))
                                            .padding(6.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = imageRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("おうちで　てを　あらったら　おしてね", color = Color(0xFF6A0DAD))
                    }
                }
            }

        }


        // 「とめる」ボタン
        if (isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isRunning = false
                    timeLeft = totalTime
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null

                    // 車の位置リセット
                    selectedCar = null
                    val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("end_time").apply()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEB3B),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 16.dp)
            ) {
                Text("とめる", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (showAnswer) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "こたえは・・・",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )

            if (revealAnswer) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentRiddle) {
                        "きゅうきゅうしゃ の うんてんしゅさん が いっぽうつうこう の みち を はんたい に あるいていました。でも、おまわりさん は おこりません。なんで？" ->
                            "うんてんじゃなくて あるいてたから！"

                        "ばなな と みかん を にだい に のせた とらっく が きゅうカーブ で なにか を おとしました。なんでしょう？" ->
                            "スピード！"

                        "てんし　が　のっている　のりもの　は　なーんだ？" ->
                            "じてんしゃ！（じ「てんし」ゃ）"

                        "119ばん　つうほう　を　うけた　しょうぼうしゃ　が　こうばん　で　とまったよ。なんでかな？" ->
                            "こうばん　が　かじ　だったから！"

                        "のるまえ　に　まずは　おりないと　いけない　のりもの　は　なーんだ？" ->
                            "ちかてつ！"

                        "「じじじじじじじじじじ　しゃ」　これは　なんの　のりものでしょう？" ->
                            "じてんしゃ！"

                        "「てん」を　つけると　ぎゅうぎゅうづめ　に　なってしまう　のりもの　なーんだ？" ->
                            "きゅうきゅうしゃ！"

                        "うえ　と　した　には　すすめるけど、まえ　と　うしろ　には　すすめない　のりもの　なーんだ？" ->
                            "エレベーター！"

                        "モォ〜　となく　どうぶつが　のっている　くるまは　しょうぼうしゃ　と　パトカー　の　どっち？" ->
                            "しょうぼうしゃ！（しょうぼ「うし」ゃ）"

                        "とまっている　ときも　うごいていないと　いけない　くるまは　なーんだ？" ->
                            "ミキサーしゃ！（ミキサーを　くるくる　まわさないと　コンクリートが　かたまっちゃうよ"

                        "べろが　まっくろな　のりもの　なーんだ？" ->
                            "タンクローリー！（タンがクロ）"

                        else -> "こたえがわかりませんでした…"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF004D40),
                    modifier = Modifier.padding(16.dp)
                )
                Button(
                    onClick = {
                        riddleMode = false
                        currentRiddle = ""
                        showAnswer = false
                        revealAnswer = false
                        selectedCar = null        // ← 車アイコンを消す
                        totalTime = 1             // ← タイマーの初期化
                        timeLeft = 0
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF90CAF9), // 水色ボタン
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp)
                ) {
                    Text("もどる", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

    }
}
}