package com.example.ai

import android.util.Log
import com.example.data.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class AiParsedTransaction(
    val classification: String, // "MERCHANT", "P2P", "SELF", "CREDIT_CARD", "CREDIT", "REFUND", "INTIMATION", "AD", "OTP", "OTHER"
    val isExpense: Boolean,     // true only for outgoing expenditure types
    val isRefund: Boolean = false, // true for REFUND classification
    val type: ExpenseType,      // MERCHANT, P2P, SELF, or CREDIT_CARD
    val amount: Double,
    val currency: String,
    val merchant: String,
    val accountInfo: String,
    val category: String,
    val isAiClassified: Boolean,
    val rawText: String
)

data class CategorizationResult(
    val category: String,
    val isAiClassified: Boolean,
    val confidence: Float,
    val errorMessage: String? = null
)

object OpenRouterCategorizer {
    private const val TAG = "OpenRouterCategorizer"
    const val DEFAULT_MODEL = "google/gemini-3.5-flash-lite"
    const val FALLBACK_MODEL = "google/gemini-2.5-flash-lite"

    // Standard SAVIO spend categories
    val KNOWN_CATEGORIES = listOf(
        "Transfers",
        "Credit Card Bill",
        "Self",
        "Groceries",
        "Food & Dining",
        "Shopping",
        "Bills & Utilities",
        "Travel & Commute",
        "Entertainment",
        "Health & Wellness",
        "Investments",
        "Education",
        "Personal Care"
    )

    /**
     * Normalizes raw category string to standard SAVIO categories.
     * Uses semantic keyword inference if category is unknown or generic.
     */
    fun normalizeCategory(rawCat: String, merchant: String = "", rawText: String = ""): String {
        val clean = rawCat.trim()
        val lowerCat = clean.lowercase(Locale.US)
        val combined = "$clean $merchant $rawText".lowercase(Locale.US)

        return when {
            // 1. Digital Services, Software Subscriptions, Cloud APIs, Developer Tools & Utilities (HIGHEST PRIORITY)
            // Even if raw LLM/bank output says "Shopping", "Entertainment", or generic labels,
            // all online services and software subscriptions belong to Bills & Utilities.
            lowerCat.contains("bill") || lowerCat.contains("utilit") || lowerCat.contains("recharge") ||
                lowerCat.contains("electricity") || lowerCat.contains("power") || lowerCat.contains("water") ||
                lowerCat.contains("gas") || lowerCat.contains("lpg") || lowerCat.contains("cylinder") ||
                lowerCat.contains("piped gas") || lowerCat.contains("broadband") || lowerCat.contains("wifi") ||
                lowerCat.contains("internet") || lowerCat.contains("telecom") || lowerCat.contains("postpaid") ||
                lowerCat.contains("prepaid") || lowerCat.contains("mobile bill") || lowerCat.contains("phone bill") ||
                lowerCat.contains("landline") || lowerCat.contains("dth") || lowerCat.contains("cable") ||
                lowerCat.contains("maintenance") || lowerCat.contains("society maintenance") || lowerCat.contains("sewerage") ||
                lowerCat.contains("waste") || lowerCat.contains("municipal") || lowerCat.contains("property tax") ||
                lowerCat.contains("challan") || lowerCat.contains("service payment") ||
                lowerCat.contains("subscription") || lowerCat.contains("software") ||
                lowerCat.contains("saas") || lowerCat.contains("hosting") ||
                lowerCat.contains("domain") || lowerCat.contains("developer") ||
                lowerCat.contains("online service") || lowerCat.contains("digital service") || lowerCat.contains("storage") ||
                lowerCat.contains("vpn") || lowerCat.contains("proxy") || lowerCat.contains("license") ||
                combined.contains("openrouter") || combined.contains("railway") || combined.contains("patreon") ||
                combined.contains("exitlag") || combined.contains("torbox") || combined.contains("onedrive") ||
                combined.contains("google play") || combined.contains("play store") || combined.contains("google storage") ||
                combined.contains("google one") || combined.contains("quillbot") || combined.contains("openai") ||
                combined.contains("chatgpt") || combined.contains("claude") || combined.contains("anthropic") ||
                combined.contains("github") || combined.contains("cursor") || combined.contains("copilot") ||
                combined.contains("replit") || combined.contains("vercel") || combined.contains("netlify") ||
                combined.contains("heroku") || combined.contains("render") || combined.contains("supabase") ||
                combined.contains("firebase") || combined.contains("aws") || combined.contains("amazon web services") ||
                combined.contains("digitalocean") || combined.contains("linode") || combined.contains("cloudflare") ||
                combined.contains("godaddy") || combined.contains("namecheap") || combined.contains("hostinger") ||
                combined.contains("notion") || combined.contains("slack") || combined.contains("zoom") ||
                combined.contains("canva") || combined.contains("adobe") || combined.contains("midjourney") ||
                combined.contains("figma") || combined.contains("linear") || combined.contains("jira") ||
                combined.contains("atlassian") || combined.contains("dropbox") || combined.contains("1password") ||
                combined.contains("bitwarden") || combined.contains("nordvpn") || combined.contains("expressvpn") ||
                combined.contains("surfshark") || combined.contains("proton") || combined.contains("microsoft 365") ||
                combined.contains("office 365") || combined.contains("jetbrains") || combined.contains("grammarly") ||
                combined.contains("laundrymate") || combined.contains("urban company") || combined.contains("icloud") ||
                combined.contains("apple services") ||
                combined.contains("bescom") || combined.contains("electricity") || combined.contains("tata power") ||
                combined.contains("adani electricity") || combined.contains("mahavitaran") || combined.contains("water") ||
                combined.contains("piped gas") || combined.contains("indane") || combined.contains("hp gas") ||
                combined.contains("bharat gas") || combined.contains("broadband") || combined.contains("act fibernet") ||
                combined.contains("airtel") || combined.contains("jio") || combined.contains(" vi ") ||
                combined.contains("vi recharge") || combined.contains("vodafone") || combined.contains("bsnl") ||
                ((combined.contains("recharge") || lowerCat.contains("recharge")) && !combined.contains("metro") && !combined.contains("transit") && !combined.contains("fastag") && !combined.contains("toll")) ||
                combined.contains("dth") || combined.contains("tata play") || combined.contains("dish tv") ||
                combined.contains("sun direct") || combined.contains("software") ||
                combined.contains("saas") || combined.contains("cloud service") -> "Bills & Utilities"
            
            (lowerCat.contains("health") || lowerCat.contains("wellness") || lowerCat.contains("pharma") ||
                lowerCat.contains("pharmacy") || lowerCat.contains("chemist") || lowerCat.contains("medical") ||
                lowerCat.contains("medicine") || (lowerCat.contains("hospital") && !lowerCat.contains("hospitality")) || lowerCat.contains("clinic") ||
                lowerCat.contains("doctor") || lowerCat.contains("diagnostic") || lowerCat.contains("pathology") ||
                lowerCat.contains("dental") || lowerCat.contains("gym") || lowerCat.contains("fitness") ||
                combined.contains("pharma") || combined.contains("pharmacy") || combined.contains("chemist") ||
                combined.contains("medplus") || combined.contains("apollo") || combined.contains("1mg") ||
                combined.contains("pharmeasy") || combined.contains("netmeds") || (combined.contains("hospital") && !combined.contains("hospitality")) ||
                combined.contains("clinic") || combined.contains("doctor") || combined.contains("dr.") ||
                combined.contains("diagnostic") || combined.contains("pathology") || combined.contains("lab") ||
                combined.contains("medicine") || combined.contains("medicos") || combined.contains("dental") ||
                combined.contains("gym") || combined.contains("fitness") || combined.contains("cult.fit")) &&
                !combined.contains("hospitalit") && !lowerCat.contains("hospitalit") -> "Health & Wellness"

            lowerCat.contains("grocer") || lowerCat.contains("supermarket") || lowerCat.contains("hypermarket") ||
                lowerCat.contains("mart") || lowerCat.contains("kirana") || lowerCat.contains("provisions") ||
                lowerCat.contains("general store") || lowerCat.contains("convenience") || lowerCat.contains("vegetable") ||
                lowerCat.contains("fruit") || lowerCat.contains("dairy") || lowerCat.contains("milk") ||
                lowerCat.contains("meat") || lowerCat.contains("poultry") || lowerCat.contains("fish") ||
                lowerCat.contains("seafood") || lowerCat.contains("bazaar") || lowerCat.contains("ration") ||
                lowerCat.contains("fresh") || lowerCat.contains("daily essentials") || lowerCat.contains("instamart") ||
                combined.contains("zepto") || combined.contains("blinkit") || combined.contains("instamart") ||
                combined.contains("swiggy instamart") ||
                combined.contains("bigbasket") || combined.contains("dmart") || combined.contains("supermarket") ||
                combined.contains("hypermarket") || combined.contains("kirana") || combined.contains("provisions") ||
                combined.contains("grocery") || combined.contains("bbdaily") || combined.contains("nature's basket") ||
                combined.contains("spencer") || combined.contains("smart bazaar") || combined.contains("reliance smart") ||
                combined.contains("country delight") || combined.contains("vegetable") || combined.contains("fruit") ||
                combined.contains("dairy") || combined.contains("milk") || combined.contains("grocery store") ||
                combined.contains("general store") || combined.contains("provision store") -> "Groceries"

            lowerCat.contains("food") || lowerCat.contains("dining") || lowerCat.contains("restaurant") ||
                lowerCat.contains("cafe") || lowerCat.contains("coffee") || lowerCat.contains("bakery") ||
                lowerCat.contains("bakeries") || lowerCat.contains("bistro") || lowerCat.contains("eatery") ||
                lowerCat.contains("diner") || lowerCat.contains("kitchen") || lowerCat.contains("canteen") ||
                lowerCat.contains("dhaba") || lowerCat.contains("pub") || lowerCat.contains("hospitality") ||
                ((lowerCat.contains(" bar") || lowerCat.startsWith("bar ") || lowerCat == "bar" || lowerCat.contains("bars")) && !lowerCat.contains("barber")) ||
                lowerCat.contains("brewery") || lowerCat.contains("lounge") || lowerCat.contains("fast food") ||
                lowerCat.contains("takeaway") || lowerCat.contains("takeout") || lowerCat.contains("dessert") ||
                lowerCat.contains("patisserie") || lowerCat.contains("confectionery") || lowerCat.contains("beverage") ||
                lowerCat.contains(" tea") || lowerCat.startsWith("tea ") || lowerCat == "tea" || lowerCat.contains("chai") ||
                lowerCat.contains("tiffin") || lowerCat.contains("breakfast") ||
                lowerCat.contains("lunch") || lowerCat.contains("dinner") || lowerCat.contains("snack") ||
                lowerCat.contains("meal") ||
                (combined.contains("swiggy") && !combined.contains("instamart")) || combined.contains("zomato") || combined.contains("starbucks") ||
                combined.contains("mcdonald") || combined.contains("kfc") || combined.contains("burger king") ||
                combined.contains("domino") || combined.contains("pizza") || combined.contains("cafe") ||
                combined.contains("restaurant") || combined.contains("dining") || combined.contains("coffee") ||
                combined.contains("bakery") || combined.contains("biryani") || combined.contains("subway") ||
                combined.contains("haldiram") || combined.contains("ownly") || combined.contains("ctrlx") || combined.contains("chai") ||
                combined.contains("tea point") || combined.contains("hospitalit") -> "Food & Dining"

            lowerCat.contains("travel") || lowerCat.contains("commute") || lowerCat.contains("transport") ||
                lowerCat.contains("transit") || lowerCat.contains("cab") || lowerCat.contains("taxi") ||
                lowerCat.contains("ride") || lowerCat.contains("metro") || lowerCat.contains("subway") ||
                lowerCat.contains("train") || lowerCat.contains("railway") || lowerCat.contains("flight") ||
                lowerCat.contains("airline") || lowerCat.contains("aviation") || lowerCat.contains("airport") ||
                ((lowerCat.contains(" bus") || lowerCat.startsWith("bus ") || lowerCat == "bus" || lowerCat.contains("buses") || lowerCat.contains("bus ticket")) && !lowerCat.contains("business")) ||
                (lowerCat.contains("coach") && !lowerCat.contains("coaching")) || lowerCat.contains("auto") ||
                lowerCat.contains("rickshaw") || lowerCat.contains("fuel") || lowerCat.contains("petrol") ||
                lowerCat.contains("diesel") || lowerCat.contains("cng") || lowerCat.contains("ev charging") ||
                lowerCat.contains("parking") || lowerCat.contains("toll") || lowerCat.contains("fastag") ||
                lowerCat.contains("ferry") || lowerCat.contains("logistics") || lowerCat.contains("hotel") || lowerCat.contains("resort") ||
                combined.contains("uber") || combined.contains("ola") || combined.contains("rapido") || combined.contains("taxi") ||
                combined.contains("metro") || combined.contains("irctc") || combined.contains("flight") || combined.contains("airline") ||
                combined.contains("air india") || combined.contains("indigo") || combined.contains("spicejet") || combined.contains("vistara") ||
                combined.contains("bus") || combined.contains("redbus") || combined.contains("fuel") || combined.contains("petrol") ||
                combined.contains("diesel") || combined.contains("cng") || combined.contains("parking") || combined.contains("shell") ||
                combined.contains("hpcl") || combined.contains("bpcl") || combined.contains("iocl") || combined.contains("indian oil") ||
                combined.contains("fastag") || combined.contains("toll") || combined.contains("makemytrip") || combined.contains("goibibo") ||
                combined.contains("cleartrip") || combined.contains("easemytrip") || combined.contains("yatra") -> "Travel & Commute"

            lowerCat.contains("entertain") || lowerCat.contains("movie") || lowerCat.contains("cinema") ||
                lowerCat.contains("theatre") || lowerCat.contains("show") || lowerCat.contains("event") ||
                lowerCat.contains("concert") || lowerCat.contains("play") || lowerCat.contains("game") ||
                lowerCat.contains("gaming") || lowerCat.contains("music") || lowerCat.contains("ott") ||
                lowerCat.contains("streaming") || combined.contains("bookmyshow") || combined.contains("pvr") ||
                combined.contains("inox") || combined.contains("cinepolis") || combined.contains("netflix") ||
                combined.contains("spotify") || combined.contains("prime video") || combined.contains("hotstar") ||
                combined.contains("disney") || combined.contains("sony liv") || combined.contains("zee5") ||
                combined.contains("apple tv") || combined.contains("apple music") || combined.contains("youtube") ||
                combined.contains("steam") || combined.contains("playstation") || combined.contains("xbox") ||
                combined.contains("nintendo") || combined.contains("epic games") -> "Entertainment"

            lowerCat.contains("shop") || lowerCat.contains("retail") || lowerCat.contains("ecommerce") ||
                lowerCat.contains("e-commerce") || lowerCat.contains("apparel") || lowerCat.contains("clothing") ||
                lowerCat.contains("clothes") || lowerCat.contains("fashion") || lowerCat.contains("wear") ||
                lowerCat.contains("garment") || lowerCat.contains("footwear") || lowerCat.contains("shoes") ||
                lowerCat.contains("electronics") || lowerCat.contains("gadget") || lowerCat.contains("mobile") ||
                lowerCat.contains("computer") || lowerCat.contains("laptop") || lowerCat.contains("hardware") ||
                lowerCat.contains("appliance") || lowerCat.contains("furniture") || lowerCat.contains("home decor") ||
                lowerCat.contains("furnishing") || lowerCat.contains("mall") || lowerCat.contains("boutique") ||
                lowerCat.contains("department store") || lowerCat.contains("jewellery") || lowerCat.contains("jewelry") ||
                lowerCat.contains("watch") || lowerCat.contains("eyewear") || lowerCat.contains("opticals") ||
                lowerCat.contains("accessories") || lowerCat.contains("bag") || lowerCat.contains("luggage") ||
                lowerCat.contains("stationery") || lowerCat.contains("merchandise") ||
                combined.contains("amazon") || combined.contains("flipkart") || combined.contains("myntra") ||
                combined.contains("ajio") || combined.contains("nykaa") || combined.contains("zara") || combined.contains("nobero") ||
                combined.contains("h&m") || combined.contains("croma") || combined.contains("reliance digital") ||
                combined.contains("vijay sales") || combined.contains("ikea") || combined.contains("meesho") ||
                combined.contains("tata cliq") || combined.contains("decathlon") || combined.contains("uniqlo") ||
                combined.contains("lenskart") || combined.contains("titan") || combined.contains("tanishq") ||
                combined.contains("westside") || combined.contains("pantaloons") || combined.contains("shoppers stop") ||
                combined.contains("lifestyle") || combined.contains("mall") -> "Shopping"

            lowerCat.contains("entertain") || lowerCat.contains("movie") || lowerCat.contains("cinema") ||
                lowerCat.contains("theatre") || lowerCat.contains("theater") || lowerCat.contains("film") ||
                lowerCat.contains("multiplex") || lowerCat.contains("streaming") ||
                (lowerCat.contains(" ott") || lowerCat.startsWith("ott") || lowerCat == "ott") ||
                lowerCat.contains("music") || lowerCat.contains("concert") || lowerCat.contains("show") ||
                lowerCat.contains("event") || lowerCat.contains("gaming") || lowerCat.contains("game") ||
                lowerCat.contains("video game") || lowerCat.contains("arcade") || lowerCat.contains("sports") ||
                lowerCat.contains("stadium") || lowerCat.contains("amusement") || lowerCat.contains("theme park") ||
                lowerCat.contains("carnival") || lowerCat.contains("ticket") ||
                combined.contains("bookmyshow") || combined.contains("netflix") || combined.contains("spotify") ||
                combined.contains("prime video") || combined.contains("hotstar") || combined.contains("pvr") ||
                combined.contains("cinepolis") || combined.contains("inox") || combined.contains("district") ||
                combined.contains("cinema") || combined.contains("movie") || combined.contains("steam") ||
                combined.contains("playstation") || combined.contains("xbox") || combined.contains("riot games") ||
                combined.contains("arenanet") || combined.contains("nintendo") || combined.contains("epic games") ||
                combined.contains("youtube") || combined.contains("apple music") || combined.contains("gaana") ||
                combined.contains("jiosaavn") || combined.contains("audible") -> "Entertainment"

            lowerCat.contains("personal") ||
                (lowerCat.contains("care") && !lowerCat.contains("daycare") && !lowerCat.contains("childcare")) ||
                lowerCat.contains("salon") ||
                lowerCat.contains("spa") || lowerCat.contains("beauty") || lowerCat.contains("barber") ||
                lowerCat.contains("haircut") || lowerCat.contains("hair") || lowerCat.contains("parlour") ||
                lowerCat.contains("parlor") || lowerCat.contains("grooming") || lowerCat.contains("skincare") ||
                lowerCat.contains("cosmetic") || lowerCat.contains("makeup") || lowerCat.contains("dermatology") ||
                lowerCat.contains("massage") || lowerCat.contains("nail") || lowerCat.contains("tattoo") ||
                lowerCat.contains("waxing") || lowerCat.contains("threading") || lowerCat.contains("facial") ||
                lowerCat.contains("hygiene") ||
                combined.contains("salon") || combined.contains("spa") || combined.contains("barber") ||
                combined.contains("parlour") || combined.contains("parlor") || combined.contains("grooming") ||
                combined.contains("skincare") || combined.contains("cosmetics") || combined.contains("urban company") ||
                combined.contains("enrich") || combined.contains("tony & guy") || combined.contains("toni & guy") ||
                combined.contains("jawed habib") || combined.contains("vlcc") || combined.contains("lakme") ||
                combined.contains("bodycraft") -> "Personal Care"

            lowerCat.contains("invest") || lowerCat.contains("stock") || lowerCat.contains("mutual") ||
                lowerCat.contains("trading") || lowerCat.contains("share") || lowerCat.contains("equity") ||
                lowerCat.contains("broker") || lowerCat.contains("brokerage") || lowerCat.contains("sip") ||
                lowerCat.contains("fund") || lowerCat.contains("securities") || lowerCat.contains("demat") ||
                lowerCat.contains("deposit") || lowerCat.contains("fixed deposit") || lowerCat.contains("recurring deposit") ||
                lowerCat.contains("nps") || lowerCat.contains("ppf") || lowerCat.contains("bonds") ||
                lowerCat.contains("gold") || lowerCat.contains("bullion") || lowerCat.contains("crypto") ||
                lowerCat.contains("wealth") || lowerCat.contains("portfolio") ||
                combined.contains("zerodha") || combined.contains("groww") || combined.contains("upstox") ||
                combined.contains("mutual fund") || combined.contains("sip") || combined.contains("stocks") ||
                combined.contains("angel one") || combined.contains("smallcase") || combined.contains("indmoney") ||
                combined.contains("kuvera") || combined.contains("etmoney") || combined.contains("sharekhan") ||
                combined.contains("motilal oswal") || combined.contains("icicidirect") || combined.contains("5paisa") ||
                combined.contains("paytm money") -> "Investments"

            lowerCat.contains("educat") || lowerCat.contains("tuition") || lowerCat.contains("course") ||
                lowerCat.contains("school") || lowerCat.contains("college") || lowerCat.contains("university") ||
                lowerCat.contains("institute") || lowerCat.contains("academy") || lowerCat.contains("coaching") ||
                lowerCat.contains("class") || lowerCat.contains("learning") || lowerCat.contains("training") ||
                lowerCat.contains("exam") || lowerCat.contains("test") || lowerCat.contains("certification") ||
                lowerCat.contains("degree") || lowerCat.contains("diploma") || lowerCat.contains("admission") ||
                lowerCat.contains("fee") || lowerCat.contains("fees") || lowerCat.contains("books") ||
                lowerCat.contains("textbook") || lowerCat.contains("library") || lowerCat.contains("stationery") ||
                lowerCat.contains("study") || lowerCat.contains("kindergarten") || lowerCat.contains("daycare") ||
                combined.contains("coursera") || combined.contains("udemy") || combined.contains("unacademy") ||
                combined.contains("byju") || combined.contains("physics wallah") || combined.contains("allen") ||
                combined.contains("aakash") || combined.contains("fiitjee") || combined.contains("school") ||
                combined.contains("college") || combined.contains("university") || combined.contains("tuition") ||
                combined.contains("fee") || combined.contains("fees") || combined.contains("books") ||
                combined.contains("edx") || combined.contains("skillshare") || combined.contains("duolingo") -> "Education"

            lowerCat.contains("self") || lowerCat.contains("own account") || lowerCat.contains("self transfer") ||
                lowerCat.contains("internal transfer") || lowerCat.contains("linked account") || lowerCat.contains("to own") ||
                combined.contains("self transfer") || combined.contains("own account") ||
                combined.contains("to hdfc") || combined.contains("to kotak") || combined.contains("to dcb") ||
                combined.contains("to sbi") || combined.contains("to icici") || combined.contains("to axis") ||
                combined.contains("to own") || combined.contains("to self") || combined.contains("linked account") ||
                combined.contains("my account") || combined.contains("savings to current") || combined.contains("self a/c") -> "Self"

            lowerCat.contains("credit card") || lowerCat.contains("card dues") || lowerCat.contains("card bill") ||
                lowerCat.contains("cc bill") || lowerCat.contains("cc payment") || lowerCat.contains("card payment") ||
                lowerCat.contains("credit card payment") || lowerCat.contains("statement payment") ||
                combined.contains("towards your credit card") || combined.contains("credit card bill") ||
                combined.contains("card dues") || combined.contains("paid to cred") || combined.contains("bill payment for card") ||
                combined.contains("credit card payment") || combined.contains("card payment") || combined.contains("cc payment") ||
                combined.contains("card settlement") -> "Credit Card Bill"

            lowerCat.contains("transfer") || lowerCat.contains("p2p") || lowerCat.contains("remittance") ||
                lowerCat.contains("send money") || lowerCat.contains("sent money") || lowerCat.contains("wire") ||
                lowerCat.contains("imps") || lowerCat.contains("neft") || lowerCat.contains("rtgs") ||
                lowerCat.contains("upi transfer") || lowerCat.contains("person to person") ||
                combined.contains("transfer to") || combined.contains("sent to") || combined.contains("paid to") ||
                combined.contains("vpa") || combined.contains("upi") || combined.contains("imps") ||
                combined.contains("neft") || combined.contains("rtgs") || combined.contains("remittance") -> "Transfers"

            else -> {
                KNOWN_CATEGORIES.firstOrNull { it.equals(clean, ignoreCase = true) } ?: "Uncategorized"
            }
        }
    }

    private suspend fun executeChatCompletion(
        bearer: String,
        chosenModel: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ): OpenRouterChatResponse {
        val reasoningConfig = if (chosenModel.contains("gemini", ignoreCase = true) || chosenModel.contains("reasoning", ignoreCase = true)) {
            OpenRouterReasoning(effort = "minimal")
        } else {
            null
        }
        val request = OpenRouterChatRequest(
            model = chosenModel,
            messages = listOf(
                OpenRouterMessage(role = "system", content = systemPrompt),
                OpenRouterMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.0,
            maxTokens = maxTokens,
            reasoning = reasoningConfig
        )
        return try {
            OpenRouterClient.api.createChatCompletion(
                authorization = bearer,
                request = request
            )
        } catch (e: retrofit2.HttpException) {
            if ((e.code() == 404 || e.code() == 400) && chosenModel == DEFAULT_MODEL) {
                Log.w(TAG, "Model $chosenModel returned HTTP ${e.code()}, retrying with fallback model $FALLBACK_MODEL")
                val fallbackReasoning = OpenRouterReasoning(effort = "minimal")
                OpenRouterClient.api.createChatCompletion(
                    authorization = bearer,
                    request = request.copy(model = FALLBACK_MODEL, reasoning = fallbackReasoning)
                )
            } else {
                throw e
            }
        }
    }

    /**
     * Parses and validates an SMS message using OpenRouter (gemini-3.5-flash-lite).
     * Rigorously confirms whether message is an actual OUTGOING EXPENDITURE vs CREDIT/INTIMATION/AD/OTP.
     * Intelligently cleans and enhances merchant name, amount, account reference, and category.
     */
    suspend fun parseSmsTransaction(
        rawText: String,
        sender: String = "",
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanKey.isEmpty()) {
            return@withContext null
        }

        val bearer = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are an expert financial transaction extraction engine for Savio₹ personal expense tracker.
Your mission: Analyze the incoming SMS message, extract all transaction details accurately into STRICT JSON.

CLASSIFICATION & VALIDATION RULES:
1. "MERCHANT": An actual outgoing payment, purchase, or debit made to a store, business, vendor, restaurant, app, pharmacy, utility, or service.
   CRITICAL — PURCHASES MADE USING A CREDIT CARD ARE "MERCHANT" PURCHASES!
   When an SMS indicates that money was spent, charged, debited, or paid USING a credit card at a merchant (e.g. Swiggy, Amazon, Uber, restaurant, supermarket, pharmacy, retail store, online purchase):
   - classification is ALWAYS "MERCHANT" (NOT "CREDIT_CARD"!)
   - isExpense is true
   - merchant is the EXACT merchant/store name from the SMS (e.g. "SWIGGY BANGALORE", "AMAZON INDIA", "SANDEEP PHARMA")
   - accountInfo is the card reference, e.g. "Card ••4821"
   - category is what was bought (e.g. "Food & Dining", "Shopping", "Groceries", "Health & Wellness", "Travel & Commute"), NEVER "Credit Card Bill"!

2. "CREDIT_CARD": ONLY for paying off a credit card bill, card statement dues, or card account repayment (e.g. "Payment received towards your credit card ending 4821", "Auto-debit for credit card bill successful", "Bill payment for card ending 1234", "Paid to CRED for credit card").
   - classification is "CREDIT_CARD"
   - isExpense is true
   - merchant is the credit card bill name (e.g. "HDFC Credit Card Bill", "CRED - Credit Card Bill")
   - category is "Credit Card Bill"

3. "SELF": Transfer between user's own bank accounts (e.g. "transfer to self account", "to own account", "to own a/c", "to self a/c", "linked account", "between your accounts", "to my account", "self transfer").
   - classification is "SELF"
   - isExpense is true
   - merchant is "Self Transfer"
   - category is "Self"

4. "P2P": Outgoing money transferred to another person/contact via UPI, NEFT, IMPS, Zelle, Venmo, or wire.
   - classification is "P2P"
   - isExpense is true
   - merchant is the recipient's exact name or VPA handle (e.g. "Ramesh Kumar" or "rahul@okaxis")
   - category is "Transfers"

5. "REFUND": A confirmed refund or reversal credited back from a merchant.
   - classification is "REFUND"
   - isExpense is false
   - merchant is the exact company/service that issued the refund (e.g. "Swiggy", "Zomato", "Amazon")
   - category is "Refund"

6. "CREDIT": Salary, general deposit, interest credited. (isExpense: false)
7. "INTIMATION", "AD", "OTP", "OTHER": Non-transactional bank notifications, balance alerts, ads, OTPs. (isExpense: false)

MERCHANT EXTRACTION RULES:
- Extract the EXACT merchant, vendor, store, or recipient name VERBATIM as written in the SMS (e.g. "SANDEEP PHARMA", "SWIGGY BANGALORE", "AMAZON INDIA", "BLUE TOKAI COFFEE", "SHELL PETROL PUMP", "ZEPTO COMMERCE").
- DO NOT summarize, abbreviate, titlecase, or replace with generic brand names. Keep the exact merchant name from the SMS text because the user will assign aliases themselves in the app.
- NEVER use the bank or carrier sender name (e.g. HDFC, ICICI, SBI, AXIS, KOTAK, Bank, VM-HDFCBK) as the merchant name.
- If paying a credit card bill: use the card bill name (e.g. "HDFC Credit Card Bill" or "CRED - Credit Card Bill").
- If transferring to self: use "Self Transfer".
- If unknown: use "Merchant / Payee" or "Transfer Recipient".

ACCOUNT INFO:
- Detect card or account info with 4-digit mask (e.g. "Card ••4821", "A/c ••3391", "UPI ••9012").

CATEGORY MAPPING GUIDE (Choose the most accurate category):
- "Health & Wellness": Any pharmacy, chemist, medicine store, pharma (e.g. "SANDEEP PHARMA", "Apollo Pharmacy", "Medplus", "Tata 1mg", "PharmEasy", "Netmeds"), hospitals (e.g. "Apollo Hospital", "Fortis", "Manipal"), clinics, diagnostic labs, doctors, gyms, fitness centers ("Cult.fit"). NOTE: Differentiate medical "hospital" from "hospitality". "hospitality" is ALWAYS Food & Dining, NEVER Health & Wellness!
- "Groceries": Supermarkets, quick-commerce (Blinkit, Zepto, Instamart, Swiggy Instamart, BigBasket), kirana stores, provisions, milk, vegetables, D-Mart. CRITICAL: "Swiggy Instamart" or "Instamart" is ALWAYS Groceries, NEVER Food & Dining!
- "Food & Dining": Restaurants, cafes, food delivery (Swiggy food delivery, Zomato, Ownly), coffee shops (Starbucks), fast food (McDonald's, Domino's, KFC), bakeries, dining. CRITICAL: "Hospitality" is ALWAYS Food & Dining, NEVER Health & Wellness! Regular Swiggy food orders are Food & Dining, but Swiggy Instamart is Groceries.
- "Travel & Commute": Cabs (Uber, Ola, Rapido), public transport (Metro, IRCTC, trains), airlines (IndiGo, Air India), hotels, resorts, fuel/petrol pumps (Shell, HPCL, BPCL, Indian Oil), FASTag, tolls, parking.
- "Bills & Utilities": Electricity (BESCOM, Tata Power), water, gas (PNG/LPG), broadband/WiFi, mobile recharge/postpaid (Airtel, Jio, Vi), DTH (Tata Play), and ALL online services, digital subscriptions, SaaS, software tools, AI APIs, cloud hosting, app store purchases (OpenRouter, OpenAI, ChatGPT, Anthropic, Claude, Railway, Exitlag, Torbox, OneDrive, Google Play, Google Storage, Google One, iCloud, Apple Services, Quillbot, GitHub, Cursor, Notion, Midjourney, Canva, Adobe, Microsoft 365, AWS, DigitalOcean, Vercel, Cloudflare, Urban Company, LaundryMate, and any online purchased service or subscription).
- "Shopping": E-commerce physical goods (Amazon, Flipkart, Myntra, Ajio), clothing/apparel (Zara, H&M, Nobero), electronics (Croma, Apple Store hardware, Reliance Digital), retail stores, malls. CRITICAL: Do NOT classify online digital services, subscriptions, software, APIs, or digital platforms as Shopping — they belong to Bills & Utilities.
- "Entertainment": Movies (BookMyShow, PVR, INOX, Cinepolis), streaming subscriptions (Netflix, Spotify, Prime Video, Hotstar, YouTube), gaming (Steam, PlayStation, XBOX, Gamepass, Arenanet, Riot Games).
- "Personal Care": Salons, spas, barbers, beauty parlours, grooming, cosmetics, skincare.
- "Investments": Stock brokers (ETMoney, Zerodha, Groww, Upstox, Angel One), mutual funds, SIPs, NPS, crypto/Coin.
- "Education": School/college fees, universities, coaching/tuition, online courses (Coursera, Udemy), books.
- "Transfers": P2P transfers sent to contacts, friends, or individuals.
- "Self": Internal transfers between own bank accounts.
- "Credit Card Bill": Paying off credit card bills/statement dues.

FEW-SHOT EXAMPLES:
SMS: "Paid Rs 450.00 to SANDEEP PHARMA from A/c XX4821 on 04-Sep. UPI Ref: 445566."
Output: {"classification":"MERCHANT","amount":450.00,"currency":"₹","merchant":"SANDEEP PHARMA","accountInfo":"A/c ••4821","category":"Health & Wellness"}

SMS: "USD 10.00 debited from Card ending 4821 at OPENROUTER.AI on 04-Sep."
Output: {"classification":"MERCHANT","amount":10.00,"currency":"$","merchant":"OPENROUTER.AI","accountInfo":"Card ••4821","category":"Bills & Utilities"}

SMS: "INR 499.00 paid for Google Play subscription on Card ending 1234 on 05-Sep."
Output: {"classification":"MERCHANT","amount":499.00,"currency":"₹","merchant":"Google Play","accountInfo":"Card ••1234","category":"Bills & Utilities"}

SMS: "USD 20.00 paid to TORBOX.APP on 06-Sep from Card ending 5511."
Output: {"classification":"MERCHANT","amount":20.00,"currency":"$","merchant":"TORBOX.APP","accountInfo":"Card ••5511","category":"Bills & Utilities"}

SMS: "Debited INR 1,200.00 on HDFC Card ending 4821 at APOLLO PHARMACY on 05-Sep."
Output: {"classification":"MERCHANT","amount":1200.00,"currency":"₹","merchant":"APOLLO PHARMACY","accountInfo":"Card ••4821","category":"Health & Wellness"}

SMS: "Dear SBI User, your A/c XX3391 debited by Rs 10,000.00 on 03-Sep towards transfer to Self A/c XX8812. UPI Ref 382910."
Output: {"classification":"SELF","amount":10000.00,"currency":"₹","merchant":"Self Transfer","accountInfo":"A/c ••3391","category":"Self"}

SMS: "INR 25,000.00 debited from A/c **4821 on 04-Sep-24 to own account in ICICI Bank. UPI: 998822."
Output: {"classification":"SELF","amount":25000.00,"currency":"₹","merchant":"Self Transfer","accountInfo":"A/c ••4821","category":"Self"}

SMS: "Payment received of INR 8,500.00 towards your HDFC Bank Credit Card ending 4821 on 02-Sep."
Output: {"classification":"CREDIT_CARD","amount":8500.00,"currency":"₹","merchant":"HDFC Credit Card Bill","accountInfo":"Card ••4821","category":"Credit Card Bill"}

SMS: "Rs 15,400.00 debited from A/c ending 1234 towards payment to CRED for Credit Card bill on 05-Sep."
Output: {"classification":"CREDIT_CARD","amount":15400.00,"currency":"₹","merchant":"CRED - Credit Card Bill","accountInfo":"A/c ••1234","category":"Credit Card Bill"}

SMS: "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA on 01-Sep. Avl Limit: Rs 1,85,000.00."
Output: {"classification":"MERCHANT","amount":6890.00,"currency":"₹","merchant":"AMAZON INDIA","accountInfo":"Card ••1004","category":"Shopping"}

SMS: "Thank you for using HDFC Bank Credit Card ending 4821 for payment of Rs 1,450.00 at SWIGGY BANGALORE on 04-Sep. Avl Limit: Rs 48,250.00."
Output: {"classification":"MERCHANT","amount":1450.00,"currency":"₹","merchant":"SWIGGY BANGALORE","accountInfo":"Card ••4821","category":"Food & Dining"}

SMS: "Paid Rs 680.00 at SWIGGY INSTAMART from A/c XX4821 on 05-Sep. UPI Ref: 889900."
Output: {"classification":"MERCHANT","amount":680.00,"currency":"₹","merchant":"SWIGGY INSTAMART","accountInfo":"A/c ••4821","category":"Groceries"}

SMS: "Debited Rs 4,200.00 on Axis Card ending 1004 at TAJ HOSPITALITY on 05-Sep."
Output: {"classification":"MERCHANT","amount":4200.00,"currency":"₹","merchant":"TAJ HOSPITALITY","accountInfo":"Card ••1004","category":"Food & Dining"}

SMS: "Paid Rs 3,500.00 at APOLLO HOSPITAL on 05-Sep from A/c XX1234."
Output: {"classification":"MERCHANT","amount":3500.00,"currency":"₹","merchant":"APOLLO HOSPITAL","accountInfo":"A/c ••1234","category":"Health & Wellness"}

SMS: "Dear SBI User, your A/c XX3391 debited by Rs 5,000.00 on 03-Sep towards Transfer to Ramesh Kumar. UPI Ref 382910."
Output: {"classification":"P2P","amount":5000.00,"currency":"₹","merchant":"Ramesh Kumar","accountInfo":"A/c ••3391","category":"Transfers"}

SMS: "Rs 650.00 refunded to A/c ending 9821 from Swiggy for cancelled order. UPI Ref: 778899."
Output: {"classification":"REFUND","amount":650.00,"currency":"₹","merchant":"Swiggy","accountInfo":"A/c ••9821","category":"Refund"}

OUTPUT FORMAT (Output STRICT RAW JSON ONLY, no markdown fences, no code blocks):
{"classification":"...","amount":0.0,"currency":"₹","merchant":"...","accountInfo":"...","category":"..."}
""".trimIndent()

        val userPrompt = """
Sender: "$sender"
SMS Body: "$rawText"
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
            val response = executeChatCompletion(
                bearer = bearer,
                chosenModel = chosenModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 1000
            )

            val choice = response.choices?.firstOrNull()?.message
            val contentText = choice?.content?.trim() ?: ""
            val reasoningText = choice?.reasoning?.trim() ?: ""
            val rawResult = if (contentText.isNotBlank()) contentText else reasoningText
            Log.d(TAG, "OpenRouter full SMS parse response: '$rawResult'")

            if (rawResult.isBlank()) return@withContext null

            // Robust JSON boundary extraction — handles markdown fences, preambles, and trailing text
            val cleanedJson = rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val jsonStart = cleanedJson.indexOf('{')
            val jsonEnd = cleanedJson.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < 0 || jsonEnd <= jsonStart) return@withContext null
            val boundedJson = cleanedJson.substring(jsonStart, jsonEnd + 1)

            val json = JSONObject(boundedJson)
            val classification = json.optString("classification", "OTHER").uppercase(Locale.US)
            val rawAmount = json.opt("amount")
            val amount = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> rawAmount.replace(",", "").replace("₹", "").replace("$", "")
                    .replace("Rs.", "", ignoreCase = true).replace("Rs", "", ignoreCase = true)
                    .trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val currency = json.optString("currency", "₹").ifEmpty { "₹" }
            var merchant = json.optString("merchant", "Unknown").ifEmpty { "Unknown" }
            val accountInfo = json.optString("accountInfo", "")
            val rawCategory = json.optString("category", "General")

            val lowerText = rawText.lowercase(Locale.US)
            val isSelfDetected = classification == "SELF" ||
                lowerText.contains("to own account") || lowerText.contains("self transfer") ||
                lowerText.contains("transferred to your own") || lowerText.contains("linked account") ||
                lowerText.contains("between your accounts") || lowerText.contains("to self") ||
                lowerText.contains("transfer to self") || lowerText.contains("paid to self") ||
                lowerText.contains("to own a/c") || lowerText.contains("to self a/c") ||
                lowerText.contains("to my account") || lowerText.contains("self acc")

            val isCardBillDetected = classification == "CREDIT_CARD" ||
                lowerText.contains("towards your credit card") || lowerText.contains("towards credit card") ||
                lowerText.contains("credit card bill") || lowerText.contains("card dues") ||
                lowerText.contains("paid to cred") || lowerText.contains("payment to cred") ||
                lowerText.contains("bill payment for card") || lowerText.contains("autopay for card") ||
                (lowerText.contains("payment received") && lowerText.contains("card"))

            var category = normalizeCategory(rawCategory, merchant, rawText)
            if (category == "Uncategorized" || category.isBlank() || category.equals("General", ignoreCase = true) || category.equals("General Spend", ignoreCase = true)) {
                category = when {
                    isSelfDetected -> "Self"
                    isCardBillDetected -> "Credit Card Bill"
                    classification == "P2P" || classification == "TRANSFER" -> "Transfers"
                    else -> normalizeCategory(rawCategory, merchant, rawText)
                }
            }

            val isRefund = classification == "REFUND" || classification == "REVERSAL"
            val isExpense = (classification == "MERCHANT" || classification == "SPEND" ||
                             classification == "P2P" || classification == "TRANSFER" ||
                             classification == "SELF" || classification == "CREDIT_CARD" ||
                             classification == "DEBIT" || classification == "EXPENSE" ||
                             classification == "PAYMENT" || classification == "PURCHASE" ||
                             isSelfDetected || isCardBillDetected) && amount > 0.0

            val expenseType = when {
                isSelfDetected -> ExpenseType.SELF
                isCardBillDetected -> ExpenseType.CREDIT_CARD
                classification == "MERCHANT" || classification == "SPEND" || classification == "DEBIT" || classification == "PURCHASE" -> ExpenseType.MERCHANT
                classification == "P2P" || classification == "TRANSFER" -> ExpenseType.P2P
                category == "Self" -> ExpenseType.SELF
                category == "Credit Card Bill" -> ExpenseType.CREDIT_CARD
                category == "Transfers" -> ExpenseType.P2P
                else -> ExpenseType.MERCHANT
            }

            if (expenseType == ExpenseType.SELF) {
                category = "Self"
                if (merchant.isBlank() || merchant.equals("Unknown", ignoreCase = true) || merchant.equals("Merchant / Payee", ignoreCase = true)) {
                    merchant = "Self Transfer"
                }
            } else if (expenseType == ExpenseType.CREDIT_CARD) {
                category = "Credit Card Bill"
                if (merchant.isBlank() || merchant.equals("Unknown", ignoreCase = true) || merchant.equals("Merchant / Payee", ignoreCase = true)) {
                    merchant = "Credit Card Bill"
                }
            }

            AiParsedTransaction(
                classification = classification,
                isExpense = isExpense,
                isRefund = isRefund,
                type = expenseType,
                amount = amount,
                currency = currency,
                merchant = merchant,
                accountInfo = accountInfo,
                category = category,
                isAiClassified = true,
                rawText = rawText
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "OpenRouter HTTP ${e.code()} parse error: $errorBody", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI full parse: ${e.message}", e)
            null
        }
    }

    /**
     * Categorizes SMS text using gemini-3.5-flash-lite via OpenRouter.
     */
    suspend fun categorizeSms(
        rawText: String,
        merchant: String,
        amount: Double,
        currency: String,
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): CategorizationResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanKey.isEmpty()) {
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f
            )
        }

        val bearer = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are a precise financial transaction categorizer for the Savio₹ personal expense tracking application.
Analyze the bank SMS and extract the exact spend category.

Permitted categories:
- Health & Wellness: Pharmacies, chemists, pharma, medical stores, medicines (e.g. "SANDEEP PHARMA", "Apollo Pharmacy", "Medplus", "1mg", "PharmEasy"), hospitals, clinics, diagnostic labs, doctors, gyms, cult.fit, fitness. (Differentiate medical "hospital" from "hospitality" — hospitality is Food & Dining).
- Groceries: Supermarkets, quick-commerce (Blinkit, Zepto, Instamart, Swiggy Instamart, BigBasket), grocery, kirana, provisions, milk, vegetables. (Swiggy Instamart is Groceries, NOT Food & Dining).
- Food & Dining: Restaurants, cafes, bakeries, food delivery (Swiggy food orders, Zomato, Ownly), coffee shops, fast food, dining.
- Travel & Commute: Cabs (Uber, Ola, Rapido), public transport (Metro, IRCTC, trains), airlines, hotels, resorts, fuel/petrol pumps, FASTag, parking, tolls.
- Bills & Utilities: Electricity, water, gas, broadband, mobile recharge/postpaid (Airtel, Jio, Vi), DTH, and ALL payments for online purchased services, digital subscriptions, SaaS, software tools, AI APIs, cloud hosting, and app stores (OpenRouter, OpenAI, ChatGPT, Claude, Railway, Exitlag, Torbox, OneDrive, Google Play, Google Storage, Google One, iCloud, Apple Services, Quillbot, GitHub, Cursor, Notion, Canva, Adobe, Microsoft 365, AWS, DigitalOcean, Vercel, Cloudflare, Urban Company, LaundryMate).
- Shopping: E-commerce physical goods (Amazon, Flipkart, Myntra, Ajio), clothing/apparel, electronics, retail stores, malls. (Never classify online digital services, subscriptions, software, or API platforms as Shopping — they belong to Bills & Utilities).
- Entertainment: Movies (BookMyShow, PVR, INOX, Cinepolis, District), streaming (Netflix, Spotify, Prime Video, Hotstar, YouTube), gaming (Steam, XBOX, Riot Games, Arenanet).
- Personal Care: Salons, spas, barbers, beauty parlours, grooming, cosmetics, skincare.
- Investments: Stock brokers (Zerodha, Groww, Upstox, Angel One), mutual funds, SIPs, NPS.
- Education: School/college fees, universities, coaching/tuition, online courses, books.
- Transfers: P2P money sent to contacts, friends, or individuals.
- Self: Internal transfers between own bank accounts.
- Credit Card Bill: ONLY when paying off credit card bills/statement dues or card payments.

Rules:
1. If the message clearly belongs to one of the above categories, output ONLY the exact category name.
2. Never use "UPI" as a category. UPI is only a payment method.
3. If the message is ambiguous, generic, cannot be determined, or is not an identifiable purchase, output ONLY "UNKNOWN".
4. Do not add explanations, prefixes, punctuation or quotes.
5. CRITICAL: A purchase made USING a credit card at a store or merchant (e.g. Swiggy, Amazon, Uber, restaurant, pharmacy) MUST be categorized by what was bought (e.g. Food & Dining, Shopping, Health & Wellness). NEVER categorize a purchase as "Credit Card Bill" just because a credit card was used! Use "Credit Card Bill" ONLY when paying off the credit card bill itself.
6. "Hospitality" is Food & Dining, NOT Health & Wellness. Differentiate "hospital" (medical) from "hospitality" (dining/hotels).
7. "Swiggy Instamart" or "Instamart" is Groceries, NOT Food & Dining. Regular Swiggy food orders are Food & Dining.
""".trimIndent()

        val userPrompt = """
SMS Text: "$rawText"
Extracted Merchant/Entity: "$merchant"
Amount: $currency$amount

Output category:
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
            val response = executeChatCompletion(
                bearer = bearer,
                chosenModel = chosenModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 500
            )

            val choice = response.choices?.firstOrNull()?.message
            val contentText = choice?.content?.trim() ?: ""
            val reasoningText = choice?.reasoning?.trim() ?: ""
            val rawResult = if (contentText.isNotBlank()) contentText else reasoningText

            if (rawResult.equals("UNKNOWN", ignoreCase = true) || rawResult.isBlank()) {
                val inferred = normalizeCategory("UNKNOWN", merchant, rawText)
                return@withContext if (inferred != "Uncategorized") {
                    CategorizationResult(
                        category = inferred,
                        isAiClassified = true,
                        confidence = 0.85f
                    )
                } else {
                    CategorizationResult(
                        category = "UNKNOWN",
                        isAiClassified = true,
                        confidence = 0.0f
                    )
                }
            }

            val cleaned = rawResult.removeSurrounding("\"").removeSurrounding("'").replace("```", "").trim()
            val normalized = normalizeCategory(cleaned, merchant, rawText)

            if (normalized != "Uncategorized") {
                CategorizationResult(
                    category = normalized,
                    isAiClassified = true,
                    confidence = 0.95f
                )
            } else {
                val sanitized = cleaned.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '&' }.trim()
                if (sanitized.length in 3..25) {
                    CategorizationResult(
                        category = sanitized,
                        isAiClassified = true,
                        confidence = 0.8f
                    )
                } else {
                    CategorizationResult(
                        category = "UNKNOWN",
                        isAiClassified = true,
                        confidence = 0f
                    )
                }
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "OpenRouter HTTP ${e.code()} categorization error: $errorBody", e)
            CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = "HTTP ${e.code()}: $errorBody"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error categorizing via OpenRouter: ${e.message}", e)
            CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = e.message
            )
        }
    }
}
