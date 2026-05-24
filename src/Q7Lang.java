import java.util.*;

// =========================================================================
// Q7Lang — Sözdizimi Özeti
//
//   bağır(ifade);;           → ekrana yaz  (print)
//   bence (koşul) (((...)))  → if
//   yoksa (((...)))          → else
//   don_baba_don (koşul)     → while
//   hakli / haksiz           → true / false
//   <=>  /  <!>              → == / !=
//   [ve_de]  /  [yada]       → && / ||
//   ;;  veya  ☕              → satır sonu
//   (((  ...  )))            → kod bloğu
// =========================================================================

// ── Token Türleri ──────────────────────────────────────────────────────────
enum TT {
    SAYI, METIN, BOOL,
    ISIM,                                    // değişken adı
    ANAHTAR,                                 // keyword
    ISARET,                                  // operatör
    BITIS,                                   // ;; veya ☕
    BLOK_AC, BLOK_KAPAT,                     // ((( )))
    PAR_AC, PAR_KAPAT,                       // ( )
    SON                                      // EOF
}

// ── Token ──────────────────────────────────────────────────────────────────
class Token {
    TT     tur;
    String deger;
    int    satir, sutun;

    Token(TT tur, String deger, int satir, int sutun) {
        this.tur = tur; this.deger = deger; this.satir = satir; this.sutun = sutun;
    }
}

// ── Lexer ──────────────────────────────────────────────────────────────────
class Lexer {
    private final String  s;
    private       int     i = 0, satir = 1, sutun = 1;
    private static final Set<String> ANAHTAR = new HashSet<>(Arrays.asList(
        "bence","yoksa","don_baba_don","bagir","hakli","haksiz"
    ));

    Lexer(String s) { this.s = s; }

    private char c()    { return i < s.length() ? s.charAt(i) : '\0'; }
    private char ileri(){ return i+1 < s.length() ? s.charAt(i+1) : '\0'; }
    private char ileri2(){ return i+2 < s.length() ? s.charAt(i+2) : '\0'; }

    private void atla() {
        if (c() == '\n') { satir++; sutun = 1; } else { sutun++; }
        i++;
    }

    List<Token> tara() throws Exception {
        List<Token> liste = new ArrayList<>();
        while (c() != '\0') {
            // boşluk
            if (Character.isWhitespace(c())) { atla(); continue; }
            // yorum
            if (c()=='/' && ileri()=='/') { while (c()!='\0' && c()!='\n') atla(); continue; }

            int sl = satir, sc = sutun;

            // ☕
            if (c() == '\u2615') { liste.add(tok(TT.BITIS,"☕",sl,sc)); atla(); continue; }
            // ;;
            if (c()==';' && ileri()==';') { liste.add(tok(TT.BITIS,";;",sl,sc)); atla(); atla(); continue; }
            // (((
            if (c()=='(' && ileri()=='(' && ileri2()=='(') { liste.add(tok(TT.BLOK_AC,"(((",sl,sc)); atla();atla();atla(); continue; }
            // )))
            if (c()==')' && ileri()==')' && ileri2()==')') { liste.add(tok(TT.BLOK_KAPAT,")))",sl,sc)); atla();atla();atla(); continue; }
            // <=>
            if (c()=='<' && ileri()=='=' && ileri2()=='>') { liste.add(tok(TT.ISARET,"<=>",sl,sc)); atla();atla();atla(); continue; }
            // <!>
            if (c()=='<' && ileri()=='!' && ileri2()=='>') { liste.add(tok(TT.ISARET,"<!>",sl,sc)); atla();atla();atla(); continue; }
            // [ve_de] / [yada]
            if (c()=='[') {
                atla();
                StringBuilder sb = new StringBuilder();
                while (c()!='\0' && c()!=']') { sb.append(c()); atla(); }
                if (c()!=']') throw hata("'[' kapatilmadi",sl,sc);
                atla();
                String ic = sb.toString();
                if (!ic.equals("ve_de") && !ic.equals("yada")) throw hata("'["+ic+"]' gecersiz",sl,sc);
                liste.add(tok(TT.ISARET,"["+ic+"]",sl,sc)); continue;
            }
            // Tek karakterli operatörler
            if ("+-*/<>=".indexOf(c())>=0) { liste.add(tok(TT.ISARET,String.valueOf(c()),sl,sc)); atla(); continue; }
            // ( )
            if (c()=='(') { liste.add(tok(TT.PAR_AC,"(",sl,sc)); atla(); continue; }
            if (c()==')') { liste.add(tok(TT.PAR_KAPAT,")",sl,sc)); atla(); continue; }
            // String
            if (c()=='"') {
                atla();
                StringBuilder sb = new StringBuilder();
                while (c()!='\0' && c()!='"') { if(c()=='\n'){satir++;sutun=1;} sb.append(c()); atla(); }
                if (c()!='"') throw hata("string kapanmadi",sl,sc);
                atla();
                liste.add(tok(TT.METIN,sb.toString(),sl,sc)); continue;
            }
            // Sayı
            if (Character.isDigit(c())) {
                StringBuilder sb = new StringBuilder();
                while (c()!='\0' && (Character.isDigit(c())||c()=='.')) { sb.append(c()); atla(); }
                liste.add(tok(TT.SAYI,sb.toString(),sl,sc)); continue;
            }
            // Kelime
            if (Character.isLetter(c())||c()=='_') {
                StringBuilder sb = new StringBuilder();
                while (c()!='\0' && (Character.isLetterOrDigit(c())||c()=='_')) { sb.append(c()); atla(); }
                String w = sb.toString();
                if      (w.equals("hakli"))  liste.add(tok(TT.BOOL,"hakli",sl,sc));
                else if (w.equals("haksiz")) liste.add(tok(TT.BOOL,"haksiz",sl,sc));
                else if (ANAHTAR.contains(w)) liste.add(tok(TT.ANAHTAR,w,sl,sc));
                else                          liste.add(tok(TT.ISIM,w,sl,sc));
                continue;
            }

            throw hata("beklenmeyen karakter '" + c() + "'", sl, sc);
        }
        liste.add(tok(TT.SON,"",satir,sutun));
        return liste;
    }

    private Token tok(TT tur, String d, int sl, int sc) { return new Token(tur,d,sl,sc); }
    private Exception hata(String msg, int sl, int sc) {
        return new Exception("Lexer Hatası ["+sl+":"+sc+"]: "+msg);
    }
}

// ── AST Düğümleri ──────────────────────────────────────────────────────────
abstract class Dugum {}

class SayiDugum  extends Dugum { double deger; SayiDugum(double d) { deger=d; } }
class MetinDugum extends Dugum { String deger; MetinDugum(String d){ deger=d; } }
class BoolDugum  extends Dugum { boolean deger;BoolDugum(boolean d){ deger=d; } }
class IsimDugum  extends Dugum { String isim; int satir,sutun; IsimDugum(String i,int s,int c){isim=i;satir=s;sutun=c;} }

class IkiliDugum extends Dugum {
    Dugum sol, sag; String isaret; int satir,sutun;
    IkiliDugum(Dugum sol,String is,Dugum sag,int s,int c){this.sol=sol;isaret=is;this.sag=sag;satir=s;sutun=c;}
}

class AtamaDugum  extends Dugum { String isim; Dugum ifade;            AtamaDugum(String i,Dugum e){isim=i;ifade=e;}              }
class BagirDugum  extends Dugum { Dugum ifade;                         BagirDugum(Dugum e){ifade=e;}                              }
class BenceDugum  extends Dugum { Dugum kosul; List<Dugum> evet,hayir; BenceDugum(Dugum k,List<Dugum>e,List<Dugum>h){kosul=k;evet=e;hayir=h;} }
class DonDugum    extends Dugum { Dugum kosul; List<Dugum> govde;      DonDugum(Dugum k,List<Dugum>g){kosul=k;govde=g;}           }

// ── Parser ─────────────────────────────────────────────────────────────────
class Parser {
    private final List<Token> liste;
    private int i = 0;

    Parser(List<Token> liste) { this.liste = liste; }

    private Token su()   { return liste.get(i); }
    private Token once() { return liste.get(i-1); }
    private boolean bitti() { return su().tur == TT.SON; }
    private Token atla()    { Token t=su(); i++; return t; }
    private boolean tur(TT t)         { return su().tur==t; }
    private boolean deger(TT t,String v){ return tur(t)&&su().deger.equals(v); }

    private Token iste(TT t, String hata) throws Exception {
        if (!tur(t)) throw hataFirlat(hata);
        return atla();
    }
    private Token isteDeger(TT t, String v, String hata) throws Exception {
        if (!deger(t,v)) throw hataFirlat(hata);
        return atla();
    }
    private Exception hataFirlat(String msg) {
        return new Exception("Parser Hatası ["+su().satir+":"+su().sutun+"]: "+msg
            +"\n→ Sözdizimi hatalı, sınav kağıdın gibi dolu dolu ama anlamsız!");
    }

    List<Dugum> isle() throws Exception {
        List<Dugum> dugumler = new ArrayList<>();
        while (!bitti()) dugumler.add(cumle());
        return dugumler;
    }

    private Dugum cumle() throws Exception {
        if (deger(TT.ANAHTAR,"bagir"))        return bagirCumle();
        if (deger(TT.ANAHTAR,"bence"))        return benceCumle();
        if (deger(TT.ANAHTAR,"don_baba_don")) return donCumle();
        if (tur(TT.ISIM))                     return atamaCumle();
        throw hataFirlat("ne yazmak istediğini anlayamadım: '"+su().deger+"'");
    }

    private Dugum bagirCumle() throws Exception {
        atla(); // bagir
        isteDeger(TT.PAR_AC,"(","'bagir' sonrası '(' bekleniyor");
        Dugum d = ifade();
        isteDeger(TT.PAR_KAPAT,")","')' bekleniyor");
        iste(TT.BITIS,"satır sonu ';;' veya '☕' bekleniyor");
        return new BagirDugum(d);
    }

    private Dugum benceCumle() throws Exception {
        atla(); // bence
        isteDeger(TT.PAR_AC,"(","'bence' sonrası '(' bekleniyor");
        Dugum kosul = ifade();
        isteDeger(TT.PAR_KAPAT,")","')' bekleniyor");
        List<Dugum> evet = blok();
        List<Dugum> hayir = null;
        if (deger(TT.ANAHTAR,"yoksa")) { atla(); hayir = blok(); }
        return new BenceDugum(kosul,evet,hayir);
    }

    private Dugum donCumle() throws Exception {
        atla(); // don_baba_don
        isteDeger(TT.PAR_AC,"(","'don_baba_don' sonrası '(' bekleniyor");
        Dugum kosul = ifade();
        isteDeger(TT.PAR_KAPAT,")","')' bekleniyor");
        return new DonDugum(kosul, blok());
    }

    private Dugum atamaCumle() throws Exception {
        Token isim = atla();
        isteDeger(TT.ISARET,"=","'=' bekleniyor");
        Dugum d = ifade();
        iste(TT.BITIS,"satır sonu ';;' veya '☕' bekleniyor");
        return new AtamaDugum(isim.deger, d);
    }

    private List<Dugum> blok() throws Exception {
        iste(TT.BLOK_AC,"'(((' bekleniyor");
        List<Dugum> liste = new ArrayList<>();
        while (!tur(TT.BLOK_KAPAT) && !bitti()) liste.add(cumle());
        iste(TT.BLOK_KAPAT,"')))' ile kapatılmalı — kapanmayan parantez gibi askıda kaldı!");
        return liste;
    }

    // İfade öncelik zinciri: [yada] → [ve_de] → <=> <!> → < > → + - → * / → primary
    private Dugum ifade() throws Exception { return yada(); }

    private Dugum yada() throws Exception {
        Dugum sol = veya();
        while (deger(TT.ISARET,"[yada]")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,veya(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum veya() throws Exception {
        Dugum sol = esitlik();
        while (deger(TT.ISARET,"[ve_de]")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,esitlik(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum esitlik() throws Exception {
        Dugum sol = karsilastir();
        while (deger(TT.ISARET,"<=>") || deger(TT.ISARET,"<!>")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,karsilastir(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum karsilastir() throws Exception {
        Dugum sol = topla();
        while (deger(TT.ISARET,"<") || deger(TT.ISARET,">")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,topla(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum topla() throws Exception {
        Dugum sol = carp();
        while (deger(TT.ISARET,"+") || deger(TT.ISARET,"-")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,carp(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum carp() throws Exception {
        Dugum sol = tekli();
        while (deger(TT.ISARET,"*") || deger(TT.ISARET,"/")) { Token op=atla(); sol=new IkiliDugum(sol,op.deger,tekli(),op.satir,op.sutun); }
        return sol;
    }
    private Dugum tekli() throws Exception {
        if (deger(TT.ISARET,"-")) { Token op=atla(); return new IkiliDugum(new SayiDugum(0),"-",tekli(),op.satir,op.sutun); }
        return temel();
    }
    private Dugum temel() throws Exception {
        Token t = su();
        if (tur(TT.SAYI))  { atla(); return new SayiDugum(Double.parseDouble(t.deger)); }
        if (tur(TT.METIN)) { atla(); return new MetinDugum(t.deger); }
        if (tur(TT.BOOL))  { atla(); return new BoolDugum(t.deger.equals("hakli")); }
        if (tur(TT.ISIM))  { atla(); return new IsimDugum(t.deger,t.satir,t.sutun); }
        if (tur(TT.PAR_AC)){ atla(); Dugum d=ifade(); isteDeger(TT.PAR_KAPAT,")","')' bekleniyor"); return d; }
        throw hataFirlat("geçersiz ifade: '"+t.deger+"'");
    }
}

// ── Sembol Tablosu ─────────────────────────────────────────────────────────
class Tablo {
    private final Map<String,Object> map = new HashMap<>();
    private final Tablo ust;
    Tablo()          { ust=null; }
    Tablo(Tablo ust) { this.ust=ust; }

    void koy(String isim, Object deger) {
        if (map.containsKey(isim))          map.put(isim,deger);
        else if (ust!=null && ust.var(isim)) ust.koy(isim,deger);
        else                                map.put(isim,deger);
    }
    Object al(String isim, int satir, int sutun) throws Exception {
        if (map.containsKey(isim)) return map.get(isim);
        if (ust!=null) return ust.al(isim,satir,sutun);
        throw new Exception("Çalışma Hatası ["+satir+":"+sutun+"]: '"+isim+"' tanımlı değil."
            +"\n→ Tanımlanmamış değişken! Sınavda boş bırakılan soru gibi — 0 puan.");
    }
    boolean var(String isim) { return map.containsKey(isim)||(ust!=null&&ust.var(isim)); }
}

// ── Evaluator ──────────────────────────────────────────────────────────────
class Evaluator {
    private Tablo tablo = new Tablo();

    void calistir(List<Dugum> dugumler) throws Exception {
        for (Dugum d : dugumler) isle(d);
    }

    private void isle(Dugum d) throws Exception {
        if (d instanceof AtamaDugum) {
            AtamaDugum a = (AtamaDugum)d;
            tablo.koy(a.isim, hesapla(a.ifade));
        }
        else if (d instanceof BagirDugum) {
            System.out.println(yazdir(hesapla(((BagirDugum)d).ifade)));
        }
        else if (d instanceof BenceDugum) {
            BenceDugum b = (BenceDugum)d;
            Object k = hesapla(b.kosul);
            if (!(k instanceof Boolean))
                throw new Exception("Çalışma Hatası: 'bence' koşulu hakli/haksiz olmalı!\n→ Karar veremiyorsun, işte bu yüzden proje notun düşük!");
            Tablo eski = tablo; tablo = new Tablo(eski);
            if ((Boolean)k) for (Dugum s : b.evet) isle(s);
            else if (b.hayir!=null) for (Dugum s : b.hayir) isle(s);
            tablo = eski;
        }
        else if (d instanceof DonDugum) {
            DonDugum dn = (DonDugum)d;
            int limit = 0;
            while (true) {
                Object k = hesapla(dn.kosul);
                if (!(k instanceof Boolean))
                    throw new Exception("Çalışma Hatası: 'don_baba_don' koşulu hakli/haksiz olmalı!\n→ Sonsuz döngüye girdin, hoca not kırıyor!");
                if (!(Boolean)k) break;
                if (++limit > 100_000)
                    throw new Exception("Çalışma Hatası: 100.000 tur döndü, hâlâ bitmedi.\n→ Döngüden çıkamıyorsun, proje gibi!");
                Tablo eski = tablo; tablo = new Tablo(eski);
                for (Dugum s : dn.govde) isle(s);
                tablo = eski;
            }
        }
    }

    private Object hesapla(Dugum d) throws Exception {
        if (d instanceof SayiDugum)  return ((SayiDugum)d).deger;
        if (d instanceof MetinDugum) return ((MetinDugum)d).deger;
        if (d instanceof BoolDugum)  return ((BoolDugum)d).deger;
        if (d instanceof IsimDugum)  {
            IsimDugum v=(IsimDugum)d; return tablo.al(v.isim,v.satir,v.sutun);
        }
        if (d instanceof IkiliDugum) {
            IkiliDugum b = (IkiliDugum)d;

            // Kısa devre
            if (b.isaret.equals("[yada]"))  { Object l=hesapla(b.sol); bekle(l,b,"[yada]"); if((Boolean)l)return true;  Object r=hesapla(b.sag); bekle(r,b,"[yada]"); return r; }
            if (b.isaret.equals("[ve_de]")) { Object l=hesapla(b.sol); bekle(l,b,"[ve_de]");if(!(Boolean)l)return false; Object r=hesapla(b.sag); bekle(r,b,"[ve_de]");return r; }

            Object sol = hesapla(b.sol), sag = hesapla(b.sag);
            switch (b.isaret) {
                case "+":
                    if (sol instanceof String || sag instanceof String) return yazdir(sol)+yazdir(sag);
                    sayi(sol,sag,b); return (Double)sol+(Double)sag;
                case "-": sayi(sol,sag,b); return (Double)sol-(Double)sag;
                case "*": sayi(sol,sag,b); return (Double)sol*(Double)sag;
                case "/":
                    sayi(sol,sag,b);
                    if((Double)sag==0) throw new Exception("Çalışma Hatası ["+b.satir+"]: Sıfıra böldün!\n→ Matematik seni affetmez, hoca da affetmez.");
                    return (Double)sol/(Double)sag;
                case "<":   sayi(sol,sag,b); return (Double)sol<(Double)sag;
                case ">":   sayi(sol,sag,b); return (Double)sol>(Double)sag;
                case "<=>": return Objects.equals(sol,sag);
                case "<!>": return !Objects.equals(sol,sag);
            }
        }
        throw new Exception("Bilinmeyen düğüm.");
    }

    private void sayi(Object l, Object r, IkiliDugum b) throws Exception {
        if (!(l instanceof Double)||!(r instanceof Double))
            throw new Exception("Çalışma Hatası ["+b.satir+"]: '"+b.isaret+"' sadece sayılarla çalışır.\n→ Elma ile armut toplanmaz, hoca da bunu bilir!");
    }
    private void bekle(Object v, IkiliDugum b, String op) throws Exception {
        if (!(v instanceof Boolean))
            throw new Exception("Çalışma Hatası ["+b.satir+"]: '"+op+"' hakli/haksiz bekliyor.\n→ Mantık sorusu sormana rağmen mantıksız cevap verdin!");
    }

    private String yazdir(Object v) {
        if (v instanceof Double) { double d=(Double)v; return d==Math.floor(d)&&!Double.isInfinite(d)?String.valueOf((long)d):String.valueOf(d); }
        if (v instanceof Boolean) return (Boolean)v?"hakli":"haksiz";
        return String.valueOf(v);
    }
}

// ── Ana Sınıf ──────────────────────────────────────────────────────────────
public class Q7Lang {
    public static void main(String[] args) {
        String kod;

        if (args.length > 0) {
            try {
                byte[] b = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0]));
                kod = new String(b, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("=== " + args[0] + " ===\n");
            } catch (java.io.IOException e) {
                System.err.println("Dosya bulunamadı: " + args[0]); return;
            }
        } else {
            kod =
                "sayac = 1;;\n" +
                "toplam = 0;;\n" +
                "don_baba_don (sayac < 6) (((\n" +
                "    toplam = toplam + sayac;;\n" +
                "    sayac = sayac + 1;;\n" +
                ")))\n" +
                "bagir(\"Toplam: \" + toplam);;\n" +
                "bence (toplam <=> 15) (((\n" +
                "    bagir(\"1+2+3+4+5=15, matematik hâlâ bozulmamış.\");;\n" +
                "))) yoksa (((\n" +
                "    bagir(\"Hesap yanlış, vizeden 0.\");;\n" +
                ")))\n" +
                "x = 3;;\n" +
                "bence (x > 1 [ve_de] x < 5) (((\n" +
                "    bagir(\"x ortalarda bir yerde: \" + x);;\n" +
                ")))\n";
            System.out.println("=== Q7Lang Demo ===\n");
        }

        try {
            List<Token>   tokenler = new Lexer(kod).tara();
            List<Dugum>   agac     = new Parser(tokenler).isle();
            new Evaluator().calistir(agac);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        System.out.println("\n=== Bitti ===");
    }
}
