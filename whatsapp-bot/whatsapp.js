require('dotenv').config();
const qrcode = require("qrcode-terminal");
const { Client } = require("whatsapp-web.js");
const express = require("express");
const bodyParser = require("body-parser");
const fetch = require("node-fetch").default;

const app = express();
app.use(bodyParser.json());

// Environment variables
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";
const FRONTEND_UPLOAD_URL = process.env.FRONTEND_URL || "https://trix-mart-upload-vercel-tawny.vercel.app";
const BOT_SECRET = process.env.BOT_SECRET || "change-this-to-a-random-string";

const client = new Client({
    puppeteer: {
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

// State management
const state = {};
const triggerKeyword = "register";
const uploadStatus = {};
const registrationTimeouts = {};

// Helpers
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const resetState = (chatId) => {
    state[chatId] = {
        keywordTriggered: false,
        step: 0,
        details: {},
        cancelled: false,
        needsIdCorrection: false,
        awaitingIdCorrection: false
    };
};

// Improved with error handling and env vars
async function isStudentIdExists(studentId) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/students/check-id?studentId=${encodeURIComponent(studentId)}`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error("Failed to check student ID:", error);
        return { exists: false };
    }
}

async function sendSafeMessage(chatId, text, delay = 0) {
    try {
        if (delay > 0) await sleep(delay);
        await client.sendMessage(chatId, text);
    } catch (error) {
        console.error('Failed to send message:', error);
    }
}

// WhatsApp client events
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

        // Clear existing timeout
        if (registrationTimeouts[chatId]) {
            clearTimeout(registrationTimeouts[chatId]);
            delete registrationTimeouts[chatId];
        }

        // New reset command
        if (userMessage.includes("reset")) {
            resetState(chatId);
            await sendSafeMessage(chatId, "♻️ Session reset. Send 'register' to start.");
            return;
        }

        // Set timeout for session
        if (userState.keywordTriggered && !userState.cancelled) {
            registrationTimeouts[chatId] = setTimeout(async () => {
                resetState(chatId);
                await sendSafeMessage(chatId, "Session timed out. Send 'register' to start again.");
            }, 30 * 60 * 1000);
        }

        if (userMessage.includes("cancel")) {
            resetState(chatId);
            await sendSafeMessage(chatId, "🚫 Registration cancelled. Send 'register' to start again.");
            return;
        }

        if (userMessage.includes("restart")) {
            resetState(chatId);
            await sendSafeMessage(chatId, "🔄 Restarting registration...");
            await initRegistrationFlow(chatId);
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
    if (userState.needsIdCorrection) {
        if (message.body.trim().toLowerCase() === "edit id") {
            userState.awaitingIdCorrection = true;
            await sendSafeMessage(chatId, "📝 Send your corrected Student ID:");
            return;
        }

        if (userState.awaitingIdCorrection) {
            const newId = message.body.trim();

            // Validate ID format (numbers only)
            if (!/^\d+$/.test(newId)) {
                await sendSafeMessage(chatId, "❌ Invalid ID (numbers only). Try again:");
                return;
            }

            const { exists } = await isStudentIdExists(newId);
            if (exists) {
                await sendSafeMessage(chatId, "❌ ID already registered. Send another one:");
                return;
            }

            userState.details.studentId = newId;
            userState.needsIdCorrection = false;
            userState.awaitingIdCorrection = false;
            await sendSafeMessage(chatId, "✅ ID updated!");
            await sendConfirmationMessage(chatId, userState);
            userState.step = 2;
            return;
        }
    }

    const details = message.body.split("\n").map(line => line.trim());

    if (details.length === 5 && details.every(d => d)) {
        const [studentId, name, businessName, businessType, subscriptionType] = details;

        // Validate student ID
        if (!/^\d+$/.test(studentId)) {
            await sendSafeMessage(chatId, "❌ Invalid Student ID (numbers only). Try again:");
            return;
        }

        const { exists } = await isStudentIdExists(studentId);
        if (exists) {
            userState.details = { studentId, name, businessName, businessType, subscriptionType };
            userState.needsIdCorrection = true;
            await sendSafeMessage(chatId, `⚠️ Student ID ${studentId} already exists.`);
            await sendSafeMessage(chatId, "Send 'edit id' to correct it or 'restart' to start over.", 1000);
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
            uploadStatus[userState.details.studentId] = chatId;
            const uploadLink = `${FRONTEND_UPLOAD_URL}?studentId=${userState.details.studentId}`;

            await sendSafeMessage(chatId, `✅ Details confirmed!`);
            await sendSafeMessage(chatId, `📷 Upload your ID here: ${uploadLink}`, 1000);
            await sendSafeMessage(chatId, "⚠️ Link expires in 5 minutes.", 1000);

            userState.step = 3; // waiting for image upload
        } catch (error) {
            console.error(error);
            await sendSafeMessage(chatId, "❌ Failed to generate upload link. Try again.");
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

// Webhook for upload confirmation
app.post('/upload-confirmation', async (req, res) => {
    try {
        if (req.headers['x-bot-secret'] !== BOT_SECRET) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        const { studentId } = req.body;
        const chatId = uploadStatus[studentId];
        if (!chatId) return res.status(400).json({ error: 'Invalid student ID' });

        await sendSafeMessage(chatId, `🎉 Upload confirmed for ID: ${studentId}`);
        await sendSafeMessage(chatId, '✅ Your registration is now complete!', 1000);

        const details = state[chatId].details;
        await fetch(`${API_BASE_URL}/api/students/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                studentId: Number(details.studentId),
                studentName: details.name,
                businessName: details.businessName,
                businessType: details.businessType,
                subscriptionType: details.subscriptionType,
                phoneNumber: details.phoneNumber,
            }),
        });

        delete uploadStatus[studentId];
        delete state[chatId];
        res.json({ success: true });
    } catch (err) {
        console.error('Webhook error:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

client.initialize();