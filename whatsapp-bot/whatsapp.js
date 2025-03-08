const qrcode = require("qrcode-terminal");
const { Client } = require("whatsapp-web.js");
const fetch = require("node-fetch");

const client = new Client();

const state = {};
const triggerKeyword = "register";

client.on("qr", qr => {
    qrcode.generate(qr, { small: true });
    console.log("Scan the QR Code above to connect the bot");
});

client.on("ready", () => {
    console.log("WhatsApp Bot is now connected and ready!");
});

client.on("message", async message => {
    const chatId = message.from;

    if (!state[chatId]?.keywordTriggered && message.body.toLowerCase().includes(triggerKeyword)) {
        state[chatId] = { keywordTriggered: true, step: 1, details: {} }; // Initialize the state for this user
        await message.reply(
            "Welcome! Let's start the registration process. Please provide your details in **one of the following formats**:\n\n"
            + "1. As a list:\n 211103063\nClaire Omua\nClaire_icl\nDesign\nPremium\n\n"
            + "2. With labels:\n*Student ID:* 211103063\n*Name:* Claire Omua\n*Business Name:* Claire_icl\n*Business Type:* Design\n*Subscription Type:* Premium\n"
        );
        return;
    }

    if (!state[chatId]?.keywordTriggered) {
        return;
    }

    const userState = state[chatId];

    // Step 1: Collect and validate user details
    if (userState.step === 1) {
        let details;

        // Handle both formats: list input or labeled input
        if (message.body.includes("\n") && message.body.includes(":")) {
            // Labeled input format (fields with "Field Name: Value")
            details = message.body.split("\n").map(line => line.split(":")[1]?.trim());
        } else {
            // Plain list format (values separated by newlines)
            details = message.body.split("\n").map(line => line.trim());
        }

        // Validate input has 5 fields
        if (details.length === 5 && details.every(detail => !!detail)) {
            [userState.details.studentId, userState.details.studentName, userState.details.businessName, userState.details.businessType, userState.details.subscriptionType] = details;

            userState.details.phoneNumber = message.from.replace("@c.us", ""); // Extract the plain phone number
            userState.step = 2;

            // Confirm details with the user
            await message.reply(
                `Please confirm your details:\n- Student ID: ${userState.details.studentId}\n- Name: ${userState.details.studentName}\n- Business Name: ${userState.details.businessName}\n- Business Type: ${userState.details.businessType}\n- Subscription Type: ${userState.details.subscriptionType}\n\nReply *yes* to confirm or send corrected details.`
            );
        } else {
            const missingFields = 5 - details.length;
            await message.reply(
                `❌ Invalid input. You seem to have provided incomplete details (${missingFields} fields missing). Please provide all details using one of the formats described earlier.`
            );
        }
    }

    // Step 2: Confirm user details
    else if (userState.step === 2) {
        if (message.body.toLowerCase() === "yes") {
            userState.step = 3;
            await message.reply("Thank you! Now please send a clear picture or PDF of your ID.");
        } else {
            userState.step = 1;
            await message.reply(
                "❌ Details not confirmed. Please resend your details in the correct format as described earlier."
            );
        }
    }

    // Step 3: Accept media for ID proof
    else if (userState.step === 3) {
        if (message.hasMedia) {
            const media = await message.downloadMedia();

            // Check for valid media type
            if (media.mimetype === "application/pdf" || media.mimetype.startsWith("image/")) {
                userState.details.idProof = media;
                userState.step = 4;

                await message.reply("✅ Thank you! Processing your registration...");

                // Send data to backend
                const payload = {
                    studentId: userState.details.studentId,
                    studentName: userState.details.studentName,
                    businessName: userState.details.businessName,
                    businessType: userState.details.businessType,
                    subscriptionType: userState.details.subscriptionType,
                    phoneNumber: userState.details.phoneNumber
                };

                try {
                    const response = await fetch("http://localhost:8080/api/students/whatsapp-webhook", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(payload)
                    });

                    if (response.ok) {
                        await message.reply("🎉 Registration successful! Your details have been saved.");
                    } else {
                        const errorResponse = await response.text();
                        await message.reply(`❌ Registration failed: ${errorResponse}`);
                    }
                } catch (error) {
                    console.error("Error communicating with backend:", error);
                    await message.reply("❌ Something went wrong while processing your registration. Please try again later.");
                }

                delete state[chatId]; // Reset the state for the user
            } else {
                await message.reply("❌ Please send a valid image or PDF.");
            }
        } else {
            await message.reply("❌ No media found. Please send a valid picture or PDF of your ID.");
        }
    }
});