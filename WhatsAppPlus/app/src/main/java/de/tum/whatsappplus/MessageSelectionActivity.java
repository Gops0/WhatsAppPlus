package de.tum.whatsappplus;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSelectionActivity extends AppCompatActivity implements View.OnClickListener {

    private String[] selectedContacts;
    private Map<String, List<Message>> selectedMessages;
    private String groupTitle;
    private int groupIcon;

    private TableLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String chatId = intent.getStringExtra(Constants.EXTRA_CHAT_ID);
        selectedContacts = intent.getStringArrayExtra(Constants.EXTRA_CONTACTS_ID);
        groupTitle = intent.getStringExtra(Constants.EXTRA_GROUP_TITLE);
        groupIcon = intent.getIntExtra(Constants.EXTRA_GROUP_ICON, R.drawable.whatsappplus_icon_group);

        final String[] selectedMessages = intent.getStringArrayExtra(Constants.EXTRA_PRE_SELECTED_MESSAGES);
        this.selectedMessages = new HashMap<>();

        List<String> contacts = new ArrayList<>();
        for (String contactId : selectedContacts) {
            Contact contact = Constants.contacts.get(contactId);

            // if one of the selected contacts is also the contact we're creating the group chat from
            if (contactId.equals(chatId)) {
                List<Message> contactsSelectedMessages = new ArrayList<>();

                // we go through the selected messages (IDs) from ChatActivity with that contact
                for (String selectedMessage : selectedMessages) {
                    // ... and through all this contact's chat messages (Message objects)
                    for (int j = 0; j < contact.chat.size(); j++) {
                        Message m = contact.chat.get(j);
                        // if the ID is found, add the message to the selected messages for this contact
                        if (selectedMessage.equals(m.id)) {
                            contactsSelectedMessages.add(m);
                        }
                    }
                }
                // put the list of selected messages in the map
                this.selectedMessages.put(contactId, contactsSelectedMessages);
            }
            if (contact.chat != null && !contact.chat.isEmpty()) {
                contacts.add(contact.name);
            }
        }

        setContentView(R.layout.activity_message_selection);

        table = (TableLayout) findViewById(R.id.messageList);

        Spinner contactSpinner = (Spinner) findViewById(R.id.contactSpinner);
        contactSpinner.getBackground().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, contacts.toArray(new String[contacts.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactSpinner.setAdapter(adapter);
        contactSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                List<Message> contactsSelectedMessages;
                if (table.getChildCount() > 0) {
                    String prevContact = (String) table.getTag(R.string.tag_chat_id);
                    contactsSelectedMessages = getSelectedMessages();
                    MessageSelectionActivity.this.selectedMessages.put(prevContact, contactsSelectedMessages);

                    table.removeAllViews();
                }

                String contactId = (String) ((TextView) selectedItemView).getText();
                table.setTag(R.string.tag_chat_id, contactId);
                List<Message> messages = Constants.contacts.get(contactId).chat;
                for (Message m : messages) {
                    View messageView = addNewChatMessage(m);
                    contactsSelectedMessages = MessageSelectionActivity.this.selectedMessages.get(contactId);
                    if (contactsSelectedMessages != null) {
                        for (Message message : contactsSelectedMessages) {
                            if (m.id.equals(message.id)) {
                                if (m.author.equals(Constants.AUTHOR_SELF)) {
                                    selectSelfMessage(true, messageView);
                                } else {
                                    selectOtherMessage(true, messageView);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("New group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public void onCreateClick(View view) {
        Constants.contacts.put(groupTitle, new Contact(groupTitle, groupIcon, getGroupMessages(), true));

        Intent startGroupChatActivity = new Intent(this, ChatActivity.class);
        startGroupChatActivity.putExtra(Constants.EXTRA_CHAT_TYPE, Constants.CHAT_TYPE_GROUP);
        startGroupChatActivity.putExtra(Constants.EXTRA_CHAT_ID, groupTitle);
        startGroupChatActivity.putExtra(Constants.EXTRA_CONTACTS_ID, selectedContacts);
        startGroupChatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(startGroupChatActivity);
    }

    private List<Message> getGroupMessages() {
        List<Message> groupMessages = new ArrayList<>();
        groupMessages.add(new Message(Constants.AUTHOR_SYSTEM, "You created group \"" + groupTitle + "\".", Constants.getCurrentTimeStamp()));
        for (String contactId : selectedContacts) {
            List<Message> contactSelectedMessages = selectedMessages.get(contactId);
            if (contactSelectedMessages != null)
                groupMessages.addAll(contactSelectedMessages);
        }
        groupMessages.add(new Message(Constants.AUTHOR_SYSTEM, "Previous messages from group creator's private chat.\nGroup chat begins here:", Constants.getCurrentTimeStamp()));
        return groupMessages;
    }

    private List<Message> getSelectedMessages() {
        List<Message> selectedMessagesInTable = new ArrayList<>();
        for (int i = 0; i < table.getChildCount(); i++) {
            View chatItem = table.getChildAt(i);
            Message message = (Message) chatItem.getTag(R.string.tag_chat_message);
            if (!Constants.AUTHOR_SYSTEM.equals(message.author)) {
                if ((boolean) chatItem.getTag(R.string.tag_selected)) {
                    selectedMessagesInTable.add(message);
                }
            }
        }
        return selectedMessagesInTable;
    }

    private View addNewChatMessage(Message message) {
        View chatItem = getLayoutInflater().inflate(R.layout.view_chat_item, table, false);
        ((TextView) chatItem.findViewById(R.id.chat_message)).setText(message.text);
        ((TextView) chatItem.findViewById(R.id.chat_timestamp)).setText(message.timeStamp);

        View chatMessageContent = chatItem.findViewById(R.id.chat_message_content);

        TableLayout.LayoutParams chatItemLayoutParams = (TableLayout.LayoutParams) chatItem.getLayoutParams();
        chatItemLayoutParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.MESSAGE_MARGIN_TOP, getResources().getDisplayMetrics());

        LinearLayout.LayoutParams chatMessageContentLayoutParams = (LinearLayout.LayoutParams) chatMessageContent.getLayoutParams();

        if (Constants.AUTHOR_SELF.equals(message.author)) {
            chatMessageContentLayoutParams.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            chatMessageContentLayoutParams.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            chatMessageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_self));
        } else {
            chatMessageContentLayoutParams.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            chatMessageContentLayoutParams.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            chatMessageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_other));
        }

        chatItem.setTag(R.string.tag_chat_message, message);
        chatItem.setTag(R.string.tag_selected, false);
        chatMessageContent.setOnClickListener(this);

        chatMessageContent.setLayoutParams(chatMessageContentLayoutParams);

        table.addView(chatItem, chatItemLayoutParams);
        return chatItem;
    }

    @Override
    public void onClick(View v) {
        selectOrDeselectMessage(v);
    }

    private void selectOrDeselectMessage(View messageContent) {
        View chatItem = (View) messageContent.getParent();
        Message message = (Message) chatItem.getTag(R.string.tag_chat_message);
        boolean chatItemSelected = (boolean) chatItem.getTag(R.string.tag_selected);
        if (Constants.AUTHOR_SELF.equals(message.author)) {
            selectSelfMessage(!chatItemSelected, chatItem);
        } else {
            selectOtherMessage(!chatItemSelected, chatItem);
        }
    }

    private void selectSelfMessage(boolean toggle, View chatItem) {
        View messageContent = chatItem.findViewById(R.id.chat_message_content);
        if (toggle) {
            messageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_self_sel));
            chatItem.setBackgroundColor(getResources().getColor(R.color.color_chat_item_background_sel));
            chatItem.setTag(R.string.tag_selected, true);
        } else {
            messageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_self));
            chatItem.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void selectOtherMessage(boolean toggle, View chatItem) {
        View messageContent = chatItem.findViewById(R.id.chat_message_content);
        if (toggle) {
            messageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_other_sel));
            chatItem.setBackgroundColor(getResources().getColor(R.color.color_chat_item_background_sel));
            chatItem.setTag(R.string.tag_selected, true);
        } else {
            messageContent.setBackground(getResources().getDrawable(R.drawable.drawable_chat_item_background_other));
            chatItem.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
    }
}
