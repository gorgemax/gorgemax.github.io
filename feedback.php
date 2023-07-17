<?php
if ($_SERVER["REQUEST_METHOD"] === "POST") {
    $toClientEmail = $_POST["email"]; // Client's email, no need for filtering as it will be validated later
    $toWebsiteOwnerEmail = "sqqq27271@gmail.com"; // Replace with the destination email address

    // Set additional headers to prevent email injection
    $headersClient = "From: " . $toWebsiteOwnerEmail . "\r\n";
    $headersClient .= "Reply-To: " . $toWebsiteOwnerEmail . "\r\n";
    $headersClient .= "X-Mailer: PHP/" . phpversion();

    $subjectClient = "Thank you for your feedback";
    $messageClient = "Thank you for your feedback!\nWe appreciate your input.";

    // Send the email to the client
    if (mail($toClientEmail, $subjectClient, $messageClient, $headersClient)) {
        // Email sent successfully to the client
        // You can add additional validation here or use third-party email validation services/APIs

        // Send the email to the website owner
        $subjectOwner = "Feedback from Website";
        $email = filter_var($_POST["email"], FILTER_SANITIZE_EMAIL);
        $feedback = filter_var($_POST["feedback"], FILTER_SANITIZE_STRING);
        $messageOwner = "Feedback from: $email\n\n";
        $messageOwner .= "Feedback:\n$feedback\n";

        $headersOwner = "From: " . $email . "\r\n";
        $headersOwner .= "Reply-To: " . $email . "\r\n";
        $headersOwner .= "X-Mailer: PHP/" . phpversion();

        if (mail($toWebsiteOwnerEmail, $subjectOwner, $messageOwner, $headersOwner)) {
            // Email sent successfully to the website owner
            echo json_encode(["status" => "success"]);
        } else {
            // Failed to send email to the website owner
            echo json_encode(["status" => "error"]);
        }
    } else {
        // Failed to send email to the client
        echo json_encode(["status" => "error"]);
    }
} else {
    echo "Invalid request method";
}
?>
