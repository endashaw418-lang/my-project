package com.smartvoice.assistant.service.command

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import com.smartvoice.assistant.data.repository.ContactRepository

/**
 * Handles phone call and messaging operations.
 * Uses Android's Intent system for calls and SmsManager for SMS.
 */
class PhoneController(private val context: Context) {

    private val contactRepository = ContactRepository(context)

    /**
     * Initiate a phone call to a contact by name.
     *
     * @return Pair of (success, resolved contact name or error message)
     */
    fun callContact(contactName: String): Pair<Boolean, String> {
        val contact = contactRepository.findContactByName(contactName)
            ?: return Pair(false, "Contact '$contactName' not found")

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${contact.phoneNumber}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(callIntent)
            Pair(true, "Calling ${contact.name}")
        } catch (e: SecurityException) {
            Pair(false, "Permission denied to make calls")
        }
    }

    /**
     * Open the dialer with a phone number.
     */
    fun dialNumber(number: String): Boolean {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(dialIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send an SMS to a contact by name.
     *
     * @return Pair of (success, status message)
     */
    fun sendMessage(contactName: String, message: String?): Pair<Boolean, String> {
        val contact = contactRepository.findContactByName(contactName)
            ?: return Pair(false, "Contact '$contactName' not found")

        return if (message != null) {
            // Send SMS directly
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Pair(true, "Message sent to ${contact.name}")
            } catch (e: Exception) {
                Pair(false, "Failed to send message: ${e.message}")
            }
        } else {
            // Open SMS app with contact pre-filled
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contact.phoneNumber}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(smsIntent)
                Pair(true, "Opening message to ${contact.name}")
            } catch (e: Exception) {
                Pair(false, "Failed to open messaging app")
            }
        }
    }

    /**
     * Open email compose with optional recipient.
     */
    fun sendEmail(recipient: String?, subject: String?): Pair<Boolean, String> {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${recipient ?: ""}")
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(emailIntent)
            Pair(true, "Opening email composer")
        } catch (e: Exception) {
            Pair(false, "No email app found")
        }
    }
}
