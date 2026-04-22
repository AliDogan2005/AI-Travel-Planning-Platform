package com.travelplanningplatform.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class EmailTemplateBuilder {

    public static String buildRegistrationEmail(String userName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #4CAF50; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to Travel Planning Platform! 🌍</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>Thank you for registering with us! We're excited to have you on board.</p>
                            <p>You can now:</p>
                            <ul>
                                <li>Create and manage your travel trips</li>
                                <li>Generate AI-powered itineraries</li>
                                <li>Search and book flights and hotels</li>
                                <li>Track your travel budget</li>
                                <li>Share trips with friends and family</li>
                            </ul>
                            <p>Happy travels! 🚀</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName);
    }

    public static String buildTripCreatedEmail(String userName, String tripName, String destination) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .trip-info { background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 15px 0; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #2196F3; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✈️ New Trip Created!</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>Great! Your new trip has been created successfully.</p>
                            <div class="trip-info">
                                <h3>Trip Details:</h3>
                                <p><strong>Trip Name:</strong> %s</p>
                                <p><strong>Destination:</strong> %s</p>
                            </div>
                            <p>Start planning your adventure and generate your AI-powered itinerary!</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, tripName, destination);
    }

    public static String buildItineraryGeneratedEmail(String userName, String tripName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #FF9800; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎯 Your Itinerary is Ready!</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>Your AI-powered itinerary for "<strong>%s</strong>" has been generated successfully!</p>
                            <p>Your personalized day-by-day itinerary is now ready to view. Start exploring your planned activities, attractions, and recommendations.</p>
                            <p>Happy exploring! 🌟</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, tripName);
    }

    public static String buildTripSharedEmail(String recipientName, String sharedByUser, String tripName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #9C27B0; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #9C27B0; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>👥 Trip Shared with You!</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p><strong>%s</strong> has shared their trip "<strong>%s</strong>" with you!</p>
                            <p>You can now collaborate on the trip, view the itinerary, and help with planning.</p>
                            <p>Check it out and start collaborating! 🎉</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, sharedByUser, tripName);
    }

    public static String buildBudgetAlertEmail(String userName, String tripName, double budget, double spent) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #F44336; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .alert { background-color: #ffebee; padding: 15px; border-left: 4px solid #F44336; margin: 15px 0; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #F44336; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⚠️ Budget Alert</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>You're approaching your budget limit for the trip "<strong>%s</strong>".</p>
                            <div class="alert">
                                <p><strong>Budget:</strong> $%.2f</p>
                                <p><strong>Amount Spent:</strong> $%.2f</p>
                                <p><strong>Remaining:</strong> $%.2f</p>
                            </div>
                            <p>Consider reviewing your expenses and adjusting your bookings if needed.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, tripName, budget, spent, (budget - spent));
    }

    public static String buildTripReminderEmail(String userName, String tripName, String startDate) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #4CAF50; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📅 Trip Reminder</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>Your trip "<strong>%s</strong>" is starting on <strong>%s</strong>!</p>
                            <p>Make sure you have:</p>
                            <ul>
                                <li>Booked your flights</li>
                                <li>Reserved your hotels</li>
                                <li>Downloaded your itinerary</li>
                                <li>Made necessary travel arrangements</li>
                            </ul>
                            <p>Enjoy your trip! 🌍✈️</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, tripName, startDate);
    }

    public static String buildGenericEmail(String subject, String content, String userName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #607D8B; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .content { padding: 20px; background-color: #f9f9f9; margin-top: 20px; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        a { color: #607D8B; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Travel Planning Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(subject, userName, content);
    }
}

