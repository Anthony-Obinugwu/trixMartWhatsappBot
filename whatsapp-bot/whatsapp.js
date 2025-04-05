const qrcode = require("qrcode-terminal");
const { Client } = require("whatsapp-web.js");
const fetch = require("node-fetch").default;

const client = new Client();
const state = {};
const triggerKeyword = "register";
const FRONTEND_UPLOAD_URL = "https://trix-mart-upload-1.vercel.app";

// Helper function to reset user state
const resetState = (chatId) => {
    delete state[chatId];
};

client.on("qr", qr => {
    qrcode.generate(qr, { small: true });
    console.log("Scan the QR Code above to connect the bot");
});

client.on("ready", () => {
    console.log("WhatsApp Bot is now connected and ready!");
});

client.on("message", async message => {
    const chatId = message.from;
    const userMessage = message.body.toLowerCase().trim();

    // Cancel/Restart command (works at any step)
    if (userMessage === 'cancel' || userMessage === 'restart') {
        resetState(chatId);
        await message.reply("🚫 Registration cancelled. Send *register* to start over.");
        return;
    }

    // Step 1: Trigger registration
    if (!state[chatId]?.keywordTriggered && userMessage.includes(triggerKeyword)) {
        state[chatId] = {
            keywordTriggered: true,
            step: 1,
            details: {},
            cancelled: false
        };
        await message.reply(
            "Welcome! Let's start the registration process. Please provide your details in **one of the following formats**:\n\n" +
            "1. As a list:\n211103063\nClaire Omua\nClaire_icl\nDesign\nPremium\n\n" +
            "2. With labels:\n*Student ID:* 211103063\n*Name:* Claire Omua\n*Business Name:* Claire_icl\n*Business Type:* Design\n*Subscription Type:* Premium\n\n" +
            "You can cancel anytime by sending *cancel*."
        );
        return;
    }

    if (!state[chatId]?.keywordTriggered || state[chatId].cancelled) return;

    const userState = state[chatId];

    // Step 2: Parse and confirm details
    if (userState.step === 1) {
        let details;
        if (message.body.includes("\n") && message.body.includes(":")) {
            details = message.body.split("\n").map(line => line.split(":")[1]?.trim());
        } else {
            details = message.body.split("\n").map(line => line.trim());
        }

        if (details.length === 5 && details.every(detail => !!detail)) {
            [userState.details.studentId, userState.details.studentName, userState.details.businessName,
                userState.details.businessType, userState.details.subscriptionType] = details;
            userState.details.phoneNumber = message.from.replace("@c.us", "");
            userState.step = 2;
            await message.reply(
                `Please confirm your details:\n- Student ID: ${userState.details.studentId}\n- Name: ${userState.details.studentName}\n` +
                `- Business Name: ${userState.details.businessName}\n- Business Type: ${userState.details.businessType}\n` +
                `- Subscription Type: ${userState.details.subscriptionType}\n\n` +
                "Reply *yes* to confirm, *edit* to correct, or *cancel* to abort."
            );
        } else {
            await message.reply("❌ Invalid format. Please provide all 5 fields as shown. Send *cancel* to abort.");
        }
    }

    // Step 3: Handle confirmation/edit
    else if (userState.step === 2) {
        if (userMessage === 'yes') {
            try {
                const createStudentResponse = await fetch("http://localhost:8080/api/students/whatsapp-webhook", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(userState.details)
                });

                if (!createStudentResponse.ok) throw new Error("Failed to create student record");

                const uploadLink = `${FRONTEND_UPLOAD_URL}?studentId=${userState.details.studentId}`;
                await message.reply(
                    `✅ Registration started! Upload your ID here:\n${uploadLink}\n\n` +
                    "*Important:* You must complete this step to finalize your registration.\n" +
                    "Send *cancel* at any time to abort."
                );
                userState.step = 3;
            } catch (error) {
                console.error(error);
                await message.reply("❌ Failed to generate upload link. Please try again later.");
            }
        }
        else if (userMessage === 'edit') {
            userState.step = 1;
            await message.reply("🔄 Please re-send your details in the requested format.");
        }
    }

    // Step 4: Post-link support
    else if (userState.step === 3) {
        await message.reply("Need help? Reply *support* or *cancel* to start over.");
    }
});

client.initialize();