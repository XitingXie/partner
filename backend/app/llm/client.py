import os
from typing import Dict, Any
from openai import OpenAI
import logging

logger = logging.getLogger(__name__)

class LLMClient:
    def __init__(self):
        print("Initializing LLM client with DeepSeek API", flush=True)
        self.client = OpenAI(api_key="sk-0b4c4bea080743b4b3672f0e6f582440",base_url="https://api.deepseek.com")

    def get_completion(self, prompt: str, message: str, temperature: float = 0.7, ) -> str:
        """
        Get a completion from the API
        
        Args:
            prompt (str): The prompt to send to the API
            temperature (float): Controls randomness in the response (0.0 to 1.0)
            
        Returns:
            str: The AI's response
        """
        print("\n=== CALLING DEEPSEEK API ===", flush=True)
        print(f"Prompt: {prompt}", flush=True)
        print(f"Message: {message}", flush=True)
        print(f"Temperature: {temperature}", flush=True)
        
        try:
            response = self.client.chat.completions.create(
                model="deepseek-chat",
                messages=[
                    {"role": "system", "content": prompt},
                    {"role": "user", "content": message}
                ],
                temperature=temperature,
                stream=False
            )
            print(f"\nAPI Response: {response}", flush=True)
            return response.choices[0].message.content
            
        except Exception as e:
            print(f"\nERROR in LLM client: {str(e)}", flush=True)
            print(f"Error type: {type(e)}", flush=True)
            raise Exception(f"Failed to get response from AI model: {str(e)}")

    # def generate_response(self, messages: list, scene_id: int = None) -> Dict[str, Any]:
    #     """
    #     Generate a response from the LLM model
    #     Args:
    #         messages: List of conversation messages
    #         scene_id: Optional scene context
    #     Returns:
    #         Dict containing response and any analysis
    #     """
    #     try:
    #         response = client.chat.completions.create(
    #             model="deepseek-chat",
    #             messages=messages,
    #             temperature=0.7,
    #             stream=False
    #         )
    #         return {
    #             "response": response.choices[0].message.content,
    #             "analysis": self.analyze_conversation(response.choices[0].message.content)
    #         }
    #     except Exception as e:
    #         print(f"Error generating response: {str(e)}")
    #         raise Exception("Failed to generate response")

    # def analyze_conversation(self, text: str) -> Dict[str, Any]:
    #     """
    #     Analyze a piece of conversation for learning opportunities
    #     Args:
    #         text: The text to analyze
    #     Returns:
    #         Dict containing various analyses
    #     """
    #     try:
    #         response = client.chat.completions.create(
    #             model="deepseek-chat",
    #             messages=[
    #                 {"role": "system", "content": "You are an AI English tutor analyzing text for learning feedback."},
    #                 {"role": "user", "content": f"Please analyze this text: {text}"}
    #             ],
    #             temperature=0.3
    #         )
    #         return response.choices[0].message.content
    #     except Exception as e:
    #         print(f"Error analyzing conversation: {str(e)}")
    #         raise Exception("Failed to analyze conversation") 