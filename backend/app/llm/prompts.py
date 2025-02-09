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
    def generate_tutor_prompt(user_level: str, scene_context: str, conversation_history: str, user_input: str, first_language: str = "zh") -> str:
        language_map = {
            "zh": "Chinese",
            "es": "Spanish",
            "pt": "Portuguese",
            "de": "German",
            "fr": "French",
            "ar": "Arabic",
            "ja": "Japanese",
            "ko": "Korean"
        }
        feedback_language = language_map.get(first_language, "Chinese")
        
        return f"""You are an English language tutor. The user's English level is {user_level}. 
            Now based on the scene {scene_context}, conversation history {conversation_history} and user input {user_input}, 
            provide feedback on the user's English usage.
            
            IMPORTANT: You must provide feedback in {feedback_language}. The user's native language is {feedback_language}, 
            so all explanations and corrections should be in {feedback_language} to ensure clear understanding.
            
            RESPOND ONLY IN THE EXACT JSON FORMAT SHOWN BELOW. 
            {{
                "feedback": {{
                    "unfamiliar_words": [],
                    "grammar_errors": {{}},
                    "not_so_good_expressions": {{}},
                    "best_fit_words": {{}}
                }},
                "tutor_message": "Your encouraging feedback message in {feedback_language}"
            }}
            
            IMPORTANT:
            1. Your response must be ONLY the JSON object above
            2. Do not add any other text before or after the JSON
            3. Use empty arrays [] or objects {{}} for categories with no issues
            4. The tutor_message MUST be in {feedback_language}
            """

    # @staticmethod
    # def get_analysis_prompt(text: str) -> str:
    #     return Prompts.ANALYSIS_TEMPLATE.format(text=text)

    @staticmethod
    def generate_partner_prompt(user_level: str, scene, conversation_history):
        return f"""You are a conversation partner. Engage in natural dialogue based on this scene:

        Scene: {scene['title']}
        Description: {scene['description']}
        Key vocabulary: {', '.join(scene['vocabulary'])}

        Previous conversation:
        {conversation_history}

        English level of the user: {user_level}

        Reply briefly but meaningfully. Keep the conversation flowing and engaging.
        """
 