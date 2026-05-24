# Q7Lang

**Q7Lang**, kod yazarken çekilen öğrenci çilelerini mizahi bir dille yansıtan özgün bir programlama dilidir.  
Sözcüksel analiz → Parser → AST → Evaluator pipeline'ı ile Java'da gerçeklenmiştir.

---

## Sözdizimi

| Yapı | Q7Lang |
|---|---|
| Ekrana yaz | `bagir("metin");;` |
| Koşul | `bence (x > 0) ((( ... )))` |
| Koşul-değilse | `yoksa ((( ... )))` |
| Döngü | `don_baba_don (x < 5) ((( ... )))` |
| Doğru / Yanlış | `hakli` / `haksiz` |
| Eşit mi? | `<=>` |
| Eşit değil mi? | `<!>` |
| Ve / Veya | `[ve_de]` / `[yada]` |
| Satır sonu | `;;` veya `☕` |
| Kod bloğu | `((( ... )))` |

---

## Kurulum

**Gereksinim:** Java 8+

```bash
# Derle
javac -encoding UTF-8 src/Q7Lang.java -d out

# Çalıştır (yerleşik demo)
java -cp out Q7Lang

# Dosyadan çalıştır
java -cp out Q7Lang examples/ornek1_merhaba.q7
```

---

## Proje Yapısı

```
Q7Lang/
├── src/
│   └── Q7Lang.java        # Tüm kaynak kod (Lexer, Parser, AST, Evaluator)
├── examples/
│   ├── ornek1_merhaba.q7  # Merhaba Dünya, aritmetik
│   ├── ornek2_kosul.q7    # bence / yoksa, mantıksal operatörler
│   └── ornek3_dongu.q7    # don_baba_don döngüsü
├── out/                   # Derleme çıktısı (.class dosyaları)
└── README.md
```

---

## Örnekler

### 1 — Merhaba Dünya

```
ad = "Dunya";;
bagir("Merhaba, " + ad);;
bagir("10 / 3 = " + (10 / 3));;
```

### 2 — Koşul

```
not = 75;;
bence (not > 69) (((
    bagir("Harf notu: BB");;
))) yoksa (((
    bagir("Harf notu: CC veya alti");;
)))
```

### 3 — Döngü

```
i = 1;;
don_baba_don (i < 4) (((
    bagir(i);;
    i = i + 1;;
)))
```

---

## Hata Mesajları

Q7Lang hataları sizi motive eder:

| Durum | Mesaj |
|---|---|
| Tanımsız değişken | `Tanımlanmamış değişken! Sınavda boş bırakılan soru gibi — 0 puan.` |
| Tip uyumsuzluğu | `Elma ile armut toplanmaz, hoca da bunu bilir!` |
| Sıfıra bölme | `Matematik seni affetmez, hoca da affetmez.` |
| Sonsuz döngü | `Döngüden çıkamıyorsun, proje gibi!` |
| Eksik `;;` | `Satır sonu ';;' veya '☕' bekleniyor` |
| Kapatılmamış `)))` | `Kapanmayan parantez gibi askıda kaldı!` |

---

## Mimari

```
Kaynak Kod (.q7)
      │
      ▼
   Lexer          → Token listesi üretir, satır/sütun takip eder
      │
      ▼
   Parser         → Recursive Descent ile AST inşa eder
      │
      ▼
   Evaluator      → AST'yi yürütür, sembol tablosunu yönetir
      │
      ▼
   Çıktı
```

**EBNF (özet):**
```
program     ::= ifade*
ifade       ::= atama | bagir | bence | don_baba_don
atama       ::= ISIM '=' expr ';;'
bagir       ::= 'bagir' '(' expr ')' ';;'
bence       ::= 'bence' '(' expr ')' blok ('yoksa' blok)?
don_baba_don::= 'don_baba_don' '(' expr ')' blok
blok        ::= '(((' ifade* ')))'
expr        ::= [yada] → [ve_de] → <=> <!> → < > → + - → * / → tekli → temel
```

---

## Operatör Önceliği

| Öncelik | Operatör |
|---|---|
| 1 (en düşük) | `[yada]` |
| 2 | `[ve_de]` |
| 3 | `<=>` `<!>` |
| 4 | `<` `>` |
| 5 | `+` `-` |
| 6 | `*` `/` |
| 7 (en yüksek) | tekli `-` |
