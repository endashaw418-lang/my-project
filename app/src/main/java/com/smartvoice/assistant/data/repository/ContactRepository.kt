package com.smartvoice.assistant.data.repository

import android.content.Context
import android.provider.ContactsContract

/**
 * Repository for accessing device contacts.
 * Used by the command executor to resolve contact names to phone numbers.
 */
class ContactRepository(private val context: Context) {

    data class Contact(
        val name: String,
        val phoneNumber: String
    )

    /**
     * Search contacts by name (case-insensitive partial match).
     */
    fun findContactByName(name: String): Contact? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactName = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phoneNumber = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                return Contact(contactName, phoneNumber)
            }
        }
        return null
    }

    /**
     * Get all contacts for autocomplete/matching.
     */
    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                contacts.add(Contact(it.getString(nameIdx), it.getString(numberIdx)))
            }
        }
        return contacts
    }
}
