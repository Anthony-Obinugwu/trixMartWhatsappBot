const qrcode = require("qrcode-terminal");
const { Client } = require("whatsapp-web.js");
const fetch = require("node-fetch").default;

const client = new Client();
const state = {};
const triggerKeyword = "register";
const FRONTEND_UPLOAD_URL = "http://localhost:63342/trixMart-amenitymanagementsystem/amenity-management/com/education/amenity/management/upload.html?_ijt=o96ja5hdb3l85lccs90cqbncp&_ij_reload=RELOAD_ON_SAVE"; // 👈 Replace with your actual URL

client.on("qr", qr => {
    qrcode.generate(qr, { small: true });
    console.log("Scan the QR Code above to connect the bot");
});

client.on("ready", () => {
    console.log("WhatsApp Bot is now connected and ready!");
});

client.on("message", async message => {
    const chatId = message.from;

    // Step 1: Trigger registration
    if (!state[chatId]?.keywordTriggered && message.body.toLowerCase().includes(triggerKeyword)) {
        state[chatId] = { keywordTriggered: true, step: 1, details: {} };
        await message.reply(
            "Welcome! Let's start the registration process. Please provide your details in **one of the following formats**:\n\n" +
            "1. As a list:\n 211103063\nClaire Omua\nClaire_icl\nDesign\nPremium\n\n" +
            "2. With labels:\n*Student ID:* 211103063\n*Name:* Claire Omua\n*Business Name:* Claire_icl\n*Business Type:* Design\n*Subscription Type:* Premium\n"
        );
        return;
    }

    if (!state[chatId]?.keywordTriggered) return;

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
                `- Subscription Type: ${userState.details.subscriptionType}\n\nReply *yes* to confirm or send corrected details.`
            );
        } else {
            await message.reply("❌ Invalid format. Please provide all 5 fields as shown in the examples.");
        }
    }

    // Step 3: Generate and send upload link
    else if (userState.step === 2 && message.body.toLowerCase() === "yes") {
        try {
            // Call backend to create student record
            const createStudentResponse = await fetch("http://localhost:8080/api/students/whatsapp-webhook", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    studentId: userState.details.studentId,
                    studentName: userState.details.studentName,
                    businessName: userState.details.businessName,
                    businessType: userState.details.businessType,
                    subscriptionType: userState.details.subscriptionType,
                    phoneNumber: userState.details.phoneNumber
                })
            });

            if (!createStudentResponse.ok) throw new Error("Failed to create student record");

            // Send upload link (now points to your frontend)
            const uploadLink = `${FRONTEND_UPLOAD_URL}?studentId=${userState.details.studentId}`;
            await message.reply(
                `✅ Registration started! Upload your ID here:\n${uploadLink}\n\n` +
                "*Important:* You must complete this step to finalize your registration."
            );
            userState.step = 3;
        } catch (error) {
            console.error(error);
            await message.reply("❌ Failed to generate upload link. Please try again later.");
        }
    }

    // Step 4: Handle post-link messages
    else if (userState.step === 3) {
        await message.reply("Need help? Reply *support* or re-send the upload link.");
    }
});

client.initialize();