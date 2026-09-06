package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Savio₹ Light-Mode Glassmorphism Palette
// ==========================================

// Primary Glassmorphic Canvas & Surfaces
val GlassBackground = Color(0xFFF8FAFC)           // Ultra-light clean slate canvas
val GlassBackgroundMesh = Color(0xFFF1F5F9)       // Soft mesh tint
val GlassSurface = Color(0xFFFFFFFF)              // Pure white base
val GlassSurfaceTranslucent = Color(0xFFFFFFFF)   // Pure white surface
val GlassCardBg = Color(0xFFFFFFFF)              // Pure white card base for seamless contrast
val GlassCardBorder = Color(0x66E2E8F0)          // Soft translucent glass border
val GlassCardBorderSubtle = Color(0x33CBD5E1)    // Ultra subtle border
val GlassPillBg = Color(0x14059669)              // Soft emerald tinted pill

// Typography & Text Tokens
val SavioSlateDark = Color(0xFF0F172A)           // Deep charcoal / slate for "Savio" and primary headings
val SavioSlateBody = Color(0xFF334155)           // Crisp slate for readable body
val SavioSlateMuted = Color(0xFF64748B)          // Secondary labels and subtitles
val SavioSlateSubtle = Color(0xFF94A3B8)         // Placeholders and subtle captions

// Brand & Savings Accent: Emerald Green
val SavioEmerald = Color(0xFF059669)             // Deep rich Emerald green for ₹ symbol and savings
val SavioEmeraldLight = Color(0xFF10B981)        // Vibrant emerald green
val SavioEmeraldContainer = Color(0xFFECFDF5)    // Soft glowing emerald glass container
val SavioEmeraldBorder = Color(0xFFA7F3D0)       // Delicate emerald border

// Financial Transaction Semantics (Clean & High Contrast)
val SavioSpendRose = Color(0xFFE11D48)           // Vivid rose-red for spends & debits
val SavioSpendRoseBg = Color(0xFFFFF1F2)         // Soft rose-red container
val SavioSpendRoseBorder = Color(0xFFFECDD3)     // Rose border

val SavioTransferIndigo = Color(0xFF4F46E5)      // Modern electric indigo for transfers
val SavioTransferIndigoBg = Color(0xFFEEF2FF)    // Soft indigo container
val SavioTransferIndigoBorder = Color(0xFFC7D2FE)// Indigo border

val SavioGoldNotes = Color(0xFFD97706)           // Warm gold/amber for rupee note wad button
val SavioGoldNotesBg = Color(0xFFFEF3C7)         // Warm gold glow container

// Blacklisted Merchant States
val SavioBlacklistRed = Color(0xFFDC2626)        // Blacklisted merchant indicator
val SavioBlacklistBg = Color(0xFFFEF2F2)         // Excluded merchant container
val SavioBlacklistMuted = Color(0xFF64748B)      // Muted strikethrough text

// Status & Activity
val StatusActiveGreen = Color(0xFF10B981)

// ==========================================
// Aliases for Smooth Compatibility
// ==========================================
val BentoPurplePrimary = SavioEmerald
val BentoPurpleDark = SavioSlateDark
val BentoLavenderCard = GlassCardBg
val BentoLavenderContainer = SavioEmeraldContainer
val BentoLavenderLight = Color(0xFFF1F5F9)
val BentoCardBg = GlassCardBg
val BentoCardBorder = Color(0xFFE2E8F0)
val BentoBorderLight = Color(0xFFF1F5F9)
val BentoDarkTile = SavioSlateDark

val BentoDebitRed = SavioSpendRose
val BentoDebitRedBg = SavioSpendRoseBg
val BentoTransferPurple = SavioTransferIndigo
val BentoTransferPurpleBg = SavioTransferIndigoBg
val BentoSpendPlum = SavioSpendRose
val BentoSpendPlumBg = SavioSpendRoseBg

val BentoBackgroundLight = GlassBackground
val BentoSurfaceLight = GlassSurface
val BentoTextPrimary = SavioSlateDark
val BentoTextSecondary = SavioSlateBody
val BentoTextTertiary = SavioSlateMuted
val BentoPillDark = SavioSlateDark

val BentoBackgroundDark = GlassBackground
val BentoSurfaceDark = GlassSurface
val BentoSurfaceVariantDark = GlassCardBg
val BentoTextDarkPrimary = SavioSlateDark
val BentoTextDarkSecondary = SavioSlateBody

val SavioLogoAccent = SavioEmerald
val SavioSavingsGreen = SavioEmerald
val SavioSavingsGreenBg = SavioEmeraldContainer
val SavioSpendRed = SavioSpendRose
val SavioSpendRedBg = SavioSpendRoseBg
