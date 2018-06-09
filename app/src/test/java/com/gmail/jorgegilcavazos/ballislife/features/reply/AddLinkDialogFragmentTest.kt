package com.gmail.jorgegilcavazos.ballislife.features.reply

import android.content.DialogInterface
import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.widget.EditText
import com.gmail.jorgegilcavazos.ballislife.BuildConfig
import com.gmail.jorgegilcavazos.ballislife.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class,
        sdk = [25],
        packageName = "com.gmail.jorgegilcavazos.ballislife")
/**
 * Tests for the
 * @see AddLinkDialogFragment
 */
class AddLinkDialogFragmentTest {

    lateinit var activity: ReplyActivity
    lateinit var resources: Resources

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(ReplyActivity::class.java)
                .create()
                .start()
                .resume()
                .get()
        resources = RuntimeEnvironment.application.resources
    }

    @Test
    fun dialogCreated() {
        val input = "Text line"
        val fragment = AddLinkDialogFragment.newInstance(input)
        fragment.show(activity.supportFragmentManager, "TAG")

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val shadowDialog = Shadows.shadowOf(dialog)

        val editText: EditText = shadowDialog.view.findViewById(R.id.edit_text)
        val editLink: EditText = shadowDialog.view.findViewById(R.id.edit_link)
        assertEquals(resources.getString(R.string.ok),
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
        assertEquals(resources.getString(R.string.cancel),
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text.toString())
        assertEquals(resources.getString(R.string.add_link), shadowDialog.title)
        assertEquals(input, editText.text.toString())
        assertTrue(editLink.text.toString().isEmpty())
    }

    @Test(expected = RuntimeException::class)
    fun ownerActivityMustImplementInteractionListener() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java)
                .create()
                .start()
                .resume()
                .get()
        val fragment = AddLinkDialogFragment.newInstance()
        fragment.show(activity.supportFragmentManager, "TAG")
    }
}