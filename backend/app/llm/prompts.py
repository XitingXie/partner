class Prompts:
    CONVERSATION_TEMPLATE = """
    Scene: {scene_description}
    Previous messages: {conversation_history}
    User: {user_message}
    Assistant: Let me help you practice this conversation scenario...
    """

    # ANALYSIS_TEMPLATE = """
    # Please analyze the following conversation for:
    # - Unfamiliar words
    # - Grammar mistakes
    # - Better expressions
    # - Best fit word suggestions

    # Text: {text}
    # """

    tutor_tasks_new_user = """
    1. Introduce the scene and provide context.
    2. Use simple, clear language to explain vocabulary, phrases, and grammar as they come up in the scene.
    3. Encourage the user to practice speaking and listening.
    4. Be friendly, supportive, and encouraging in your feedback.
    """

    tutor_tasks_returning_user = """
    1. Review unfamiliar words, not-so-good expressions, and grammar errors from previous conversations.
    2. Provide gentle corrections and suggestions for improvement.
    3. Introduce more advanced vocabulary and grammar concepts.
    4. Encourage the user to actively recall and use previously learned words or phrases.
    5. Be friendly, supportive, and encouraging in your feedback.
    """

    @staticmethod
    def generate_tutor_prompt(scene, conversation_history, tutor_tasks):
        return f"""You are an English tutor. Analyze the user's message and provide feedback in JSON format.
        
    Scene: {scene['title']}
    Setting: {scene['setting']}
    Key vocabulary: {', '.join(scene['vocabulary'])}

    Previous conversation:
    {conversation_history}

    Provide feedback in this exact JSON structure:
    {{
        "feedback": {{
            "unfamiliar_words": [],
            "not_so_good_expressions": {{}},
            "grammar_errors": {{}},
            "best_fit_words": {{}}
        }}
    }}

    Only include elements that need feedback. If there are no issues, return empty arrays/objects.
    Do not include any conversation or additional text in your response.
    """

    # @staticmethod
    # def get_analysis_prompt(text: str) -> str:
    #     return Prompts.ANALYSIS_TEMPLATE.format(text=text)

    @staticmethod
    def generate_partner_prompt(scene, conversation_history):
        return f"""You are a friendly conversation partner helping someone practice English.

            Scene: {scene['title']}
            Setting: {scene['setting']}

            Previous conversation:
            {conversation_history}

            Respond naturally as a friend. Keep the conversation flowing and engaging.
            """

# def get_chat_prompt(scene_context: str) -> str:
#     """
#     Get the prompt for chat interactions.
    
#     Args:
#         scene_context: The context/description of the current scene
        
#     Returns:
#         str: The formatted prompt
#     """
#     return f"""You are a helpful AI assistant in a language learning app. You are helping users practice conversations in the following scene:

#     {scene_context}

#     Respond in this exact JSON format:
#     {{
#         "conversation": "Your natural conversational response here",
#         "feedback": {{
#             "unfamiliar_words": ["word1", "word2"],
#             "not_so_good_expressions": {{"original": "better"}},
#             "grammar_errors": {{"incorrect": "correct"}},
#             "best_fit_words": {{"original": "better"}}
#         }}
#     }}

#     Guidelines:
#     1. Keep conversation natural and friendly
#     2. Provide feedback on language usage
#     3. Always maintain the exact JSON structure
#     4. Keep responses concise and focused
#     5. Stay in character for the scene

#     Remember: Your response MUST be valid JSON with both "conversation" and "feedback" keys."""

# def get_scene_prompt(topic: str) -> str:
#     """Get prompt for generating scene descriptions"""
#     return f"""Create an engaging conversation scene for practicing {topic}.
# Include context, example dialogs, and key phrases."""