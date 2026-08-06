package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alijafari.red.astronomy.ui.theme.IranSans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurabadHistoryModal(
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFB703).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFB703).copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Castle,
                                        contentDescription = "NC Heritage",
                                        tint = Color(0xFFFFB703),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isFa) "تاریخ باستانی نورآباد ممسنی (NC)" else "Ancient History of Nurabad Mamasani (NC)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFa) "میراث شکوهمند ایران باستان پیش از اسلام" else "Glorious Heritage of Pre-Islamic Ancient Persia",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = IranSans,
                                    color = Color(0xFFFFB703)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Scrollable History Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Introduction
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isFa) "🏛️ مهد تمدن انشان و سرزمین شکوهمند ممسنی" else "🏛️ Cradle of Anshan Civilization & Mamasani Plains",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB703)
                                )
                                Text(
                                    text = if (isFa)
                                        "نورآباد ممسنی (NC) واقع در استان فارس، یکی از کهن‌ترین و ارزشمندترین کانون‌های تمدنی ایران باستان است. این منطقه دشت حاصلخیز و راهبردی میان انشان و خوزستان بوده که قدمت سکونت در تپه‌های باستانی آن مانند تل نورآباد به بیش از ۵۰۰۰ سال پیش (عصر مفرغ و ایلام باستان) می‌رسد."
                                    else
                                        "Nurabad Mamasani (NC) in Fars province, Iran, stands as one of the oldest and most strategic civilization cradles of ancient Persia. Serving as the vital fertile corridor between Elamite Anshan and Susa, archaeology at Tell Nurabad reveals continuous human settlement exceeding 5,000 years back to the Bronze Age.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = IranSans,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // Chapter 1: Mil-e Azhdaha
                    item {
                        HistoryChapterCard(
                            icon = Icons.Default.TempleHindu,
                            title = if (isFa) "میل اژدها (Mil-e Azhdaha) — برج آتشنیان اشکانی" else "Mil-e Azhdaha — Parthian Fire Temple Tower",
                            period = if (isFa) "دوره اشکانیان (سده دوم پیش از میلاد)" else "Parthian Era (2nd Century BC)",
                            content = if (isFa)
                                "میل اژدها یا دیمه‌میل، برجی سنگی و چهارگوش به ارتفاع بیش از ۷ متر است که از سنگ‌های تراش‌خورده بدون ملات ساخته شده است. این بنا یکی از معدود برج‌های آتشکده‌ای برجا مانده از دوران اشکانیان در ایران است که نیایشگاه و راهنمای کاروان‌های جاده شاهی میان استخر و سوسا (شوش) بوده است."
                            else
                                "Mil-e Azhdaha (also known as Dimah Mil) is a remarkable 7-meter square stone tower constructed from meticulously carved blocks without mortar. It represents one of Persia's rare surviving Parthian-era fire temple towers, acting as a sacred sanctuary and navigational beacon along the Royal Road connecting Estakhr and Susa.",
                            isFa = isFa
                        )
                    }

                    // Chapter 2: Sarab-e Bahram
                    item {
                        HistoryChapterCard(
                            icon = Icons.Default.Landscape,
                            title = if (isFa) "نقش‌برجسته سراب بهرام (Sarab-e Bahram)" else "Sarab-e Bahram — Sassanian Rock Relief",
                            period = if (isFa) "دوره ساسانیان (بهرام دوم)" else "Sassanian Empire (Bahram II)",
                            content = if (isFa)
                                "در ۹ کیلومتری نورآباد، نقش‌برجسته باشکوه سراب بهرام بر سینه کوه و کنار چشمه‌ای زلال حجاری شده است. این اثر شاه بهرام دوم ساسانی را نشسته بر تخت نشان می‌دهد که بزرگان و بزرگان دربار مانند کرتیر (موبدان موبد) در دو طرف او به نشانه احترام ایستاده‌اند."
                            else
                                "Located 9 km from Nurabad, the magnificent Sarab-e Bahram rock relief is carved directly into the cliff side adjacent to a crystal spring. It depicts Sassanian King Bahram II enthroned while high court dignitaries, including Kartir the high priest, stand solemnly at either side in homage.",
                            isFa = isFa
                        )
                    }

                    // Chapter 3: Ariobarzanes & Battle of Persian Gates
                    item {
                        HistoryChapterCard(
                            icon = Icons.Default.MilitaryTech,
                            title = if (isFa) "حماسه آریوبرزن و دفاع در دربند پارس (تنگ تکاب)" else "Ariobarzanes & Battle of the Persian Gates",
                            period = if (isFa) "دوران هخامنشی (۳۳۰ پیش از میلاد)" else "Achaemenid Empire (330 BC)",
                            content = if (isFa)
                                "در دشت‌ها و تنگه‌های کوهستانی ممسنی (تنگ تکاب)، سردار نامدار هخامنشی «آریوبرزن» و خواهرش «یوتاب» با پایداری شگفت‌انگیز در برابر سپاه اسکندر مقدونی ایستادگی کردند. این نبرد حماسی نماد جاودان آزادگی و دفاع فداکارانه از خاک میهن در تاریخ ایران است."
                            else
                                "In the rugged mountain passes of Mamasani (Tang-e Takab), the legendary Achaemenid commander Ariobarzanes and his sister Youtab made their legendary last stand against Alexander the Great's army in 330 BC. Their heroic defense remains an immortal symbol of Persian valor and patriotism.",
                            isFa = isFa
                        )
                    }

                    // Chapter 4: Elamite Antiquities
                    item {
                        HistoryChapterCard(
                            icon = Icons.Default.History,
                            title = if (isFa) "تمدن ایلامی و انشان (پایه شاهنشاهی هخامنشی)" else "Elamite Heritage & Anshan Roots",
                            period = if (isFa) "هزاره سوم تا اول پیش از میلاد" else "3rd to 1st Millennium BC",
                            content = if (isFa)
                                "منطقه نورآباد ممسنی بخشی از قلمرو پادشاهی انشان (یکی از دو قطب اصلی تمدن ایلام) بوده است. کوروش بزرگ بنیان‌گذار شاهنشاهی هخامنشی خود را «شاه انشان» می‌نامید و فرهنگ و هنر این سرزمین زیربنای شکوه ایران باستان گردید."
                            else
                                "Nurabad Mamasani formed an integral core of the kingdom of Anshan, one of the two twin pillars of Elamite civilization. Cyrus the Great proudfully titled himself 'King of Anshan', establishing the foundational heritage for the Achaemenid Persian Empire.",
                            isFa = isFa
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB703),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isFa) "متوجه شدم — بازگشت به نقشه" else "Got It — Return to Map",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChapterCard(
    icon: ImageVector,
    title: String,
    period: String,
    content: String,
    isFa: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = IranSans,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = IranSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}
