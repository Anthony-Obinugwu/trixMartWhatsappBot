require('dotenv').config();
const qrcode = require("qrcode-terminal");
const { Client } = require("whatsapp-web.js");
const express = require("express");
const bodyParser = require("body-parser");
const fetch = require("node-fetch").default;

const app = express();
app.use(bodyParser.json());

const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";
const FRONTEND_UPLOAD_URL = process.env.FRONTEND_URL || "https://trix-mart-upload-vercel-tawny.vercel.app";
const BOT_SECRET = process.env.BOT_SECRET || "your-secret-key";

const client = new Client({
    puppeteer: {
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

const state = {};
const triggerKeyword = "register";

const resetState = (chatId) => {
    state[chatId] = {
        keywordTriggered: false,
        step: 0,
        details: {},
        cancelled: false
    };
};

async function sendSafeMessage(chatId, text, delay = 0) {
    try {
        if (delay > 0) await new Promise(resolve => setTimeout(resolve, delay));
        await client.sendMessage(chatId, text);
    } catch (error) {
        console.error('Failed to send message:', error);
    }
}

client.on("qr", qr => {
    qrcode.generate(qr, { small: true });
    console.log("Scan the QR Code above to connect the bot");
});

client.on("ready", () => {
    console.log("WhatsApp Bot is now connected and ready!");
});

client.on("message", async message => {
    try {
        const chatId = message.from;
        const userMessage = message.body.toLowerCase().trim();

        if (!state[chatId]) resetState(chatId);
        const userState = state[chatId];

        if (userMessage.includes("cancel")) {
            resetState(chatId);
            await sendSafeMessage(chatId, "🚫 Registration cancelled. Send 'register' to start again.");
            return;
        }

        if (!userState.keywordTriggered && userMessage.includes(triggerKeyword)) {
            await initRegistrationFlow(chatId);
            return;
        }

        if (userState.step === 1) {
            await handleStep1(message, userState, chatId);
        } else if (userState.step === 2) {
            await handleStep2(message, userState, chatId);
        }
    } catch (error) {
        console.error("Error handling message:", error);
    }
});

async function handleStep1(message, userState, chatId) {
    const details = message.body.split("\n").map(line => line.trim());

    if (details.length === 5 && details.every(d => d)) {
        const [studentId, name, businessName, businessType, subscriptionType] = details;

        if (!/^\d+$/.test(studentId)) {
            await sendSafeMessage(chatId, "❌ Invalid Student ID (numbers only). Try again:");
            return;
        }

        userState.details = {
            studentId,
            name,
            businessName,
            businessType,
            subscriptionType,
            phoneNumber: message.from.replace("@c.us", "")
        };

        await sendConfirmationMessage(chatId, userState);
        userState.step = 2;
    } else {
        await sendSafeMessage(chatId, "❌ Invalid format. Send your details like this:");
        await sendSafeMessage(chatId, "Example:\n211103063\nClaire Omua\nClaire_icl\nDesign\nPremium", 1000);
    }
}

async function handleStep2(message, userState, chatId) {
    const response = message.body.trim().toLowerCase();

    if (response === 'yes') {
        try {
            const res = await fetch(`${API_BASE_URL}/api/students/whatsapp-registration`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    studentId: userState.details.studentId,
                    studentName: userState.details.name,
                    businessName: userState.details.businessName,
                    businessType: userState.details.businessType,
                    subscriptionType: userState.details.subscriptionType,
                    phoneNumber: userState.details.phoneNumber
                })
            });

            if (!res.ok) {
                const errorData = await res.text();
                throw new Error(errorData || 'Failed to save to database');
            }

            const responseData = await res.json();

            await sendSafeMessage(chatId, "✅ Details stored, please upload your ID to complete registration");

            const uploadLink = `${FRONTEND_UPLOAD_URL}`;
            await sendSafeMessage(chatId, `📷 Upload your ID here: ${uploadLink}`, 1000);
            await sendSafeMessage(chatId, "⚠️ Link expires in 5 minutes.", 1000);

            resetState(chatId);
        } catch (error) {
            console.error(error);
            await sendSafeMessage(chatId, `❌ Error: ${error.message}`);
        }
    } else if (response === 'edit') {
        userState.step = 1;
        await sendSafeMessage(chatId, "🔄 Please resend your details in list format.");
    }
}

async function initRegistrationFlow(chatId) {
    resetState(chatId);
    state[chatId].keywordTriggered = true;
    state[chatId].step = 1;

    await sendSafeMessage(chatId, "👋 Welcome to registration!");
    await sendSafeMessage(chatId, "Please send your details in this format:", 1000);
    await sendSafeMessage(chatId, "Example:\n211103063\nClaire Omua\nClaire_icl\nDesign\nPremium", 1000);
}

async function sendConfirmationMessage(chatId, userState) {
    const { studentId, name, businessName, businessType, subscriptionType } = userState.details;

    await sendSafeMessage(chatId, "Please confirm your details:");
    await sendSafeMessage(chatId,
        `Student ID: ${studentId}\n` +
        `Name: ${name}\n` +
        `Business: ${businessName}\n` +
        `Type: ${businessType}\n` +
        `Subscription: ${subscriptionType}`,
        500
    );
    await sendSafeMessage(chatId, "Reply 'yes' to confirm or 'edit' to change.", 500);
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

client.initialize();