import os
from typing import Dict, Any
from openai import OpenAI, APIError, APIConnectionError, RateLimitError, APITimeoutError
import logging
import traceback
from ollama import chat
from ollama import ChatResponse

logger = logging.getLogger(__name__)

class LLMClient:
    def __init__(self):
        print("Initializing LLM client with DeepSeek API", flush=True)
        self.client = OpenAI(
            api_key="sk-0b4c4bea080743b4b3672f0e6f582440",
            base_url="https://api.deepseek.com"
        )

    def get_completion(self, prompt: str, message: str, temperature: float = 0.7) -> str:
        """
        Get a completion from the API
        
        Args:
            prompt (str): The system prompt
            message (str): The user message
            temperature (float): Controls randomness in the response (0.0 to 1.0)
            
        Returns:
            str: The AI's response
        """
        try:
            print("\n=== CALLING DEEPSEEK API ===", flush=True)
            print(f"Prompt length: {len(prompt)}", flush=True)
            print(f"Message length: {len(message)}", flush=True)
            print(f"Temperature: {temperature}", flush=True)

            # Truncate prompt and message if they're extremely long
            truncated_prompt = prompt[:5000] if len(prompt) > 5000 else prompt
            truncated_message = message[:1000] if len(message) > 1000 else message

            # response = self.client.chat.completions.create(
            #     model="deepseek-chat",
            #     messages=[
            #         {"role": "system", "content": truncated_prompt},
            #         {"role": "user", "content": truncated_message}
            #     ],
            #     temperature=temperature,
            #     stream=False,
            #     timeout=30.0  # Explicit float timeout
            # )
            response: ChatResponse = chat(
                model='deepseek-v2', 
                messages=[
                    {"role": "system", "content": truncated_prompt},
                    {"role": "user", "content": truncated_message}
                ],
                stream=False,
            )
            
            print(f"\nAPI Response received", flush=True)
            ai_response = response.message.content
            # ai_response = response.choices[0].message.content
            print(f"AI Response: {(ai_response)}", flush=True)
            return ai_response
            
        except APIConnectionError as e:
            error_msg = f"Failed to connect to API: {str(e)}"
            print(f"\nAPI CONNECTION ERROR: {error_msg}", flush=True)
            print(f"Traceback: {traceback.format_exc()}", flush=True)
            raise Exception(error_msg)
        
        except APITimeoutError as e:
            error_msg = f"API request timed out: {str(e)}"
            print(f"\nAPI TIMEOUT ERROR: {error_msg}", flush=True)
            print(f"Traceback: {traceback.format_exc()}", flush=True)
            raise Exception(error_msg)
        
        except RateLimitError as e:
            error_msg = f"API rate limit exceeded: {str(e)}"
            print(f"\nRATE LIMIT ERROR: {error_msg}", flush=True)
            print(f"Traceback: {traceback.format_exc()}", flush=True)
            raise Exception(error_msg)
        
        except APIError as e:
            error_msg = f"API returned an error: {str(e)}"
            print(f"\nAPI ERROR: {error_msg}", flush=True)
            print(f"Traceback: {traceback.format_exc()}", flush=True)
            raise Exception(error_msg)
        
        except Exception as e:
            error_msg = f"Unexpected error in LLM client: {str(e)}"
            print(f"\nUNEXPECTED ERROR: {error_msg}", flush=True)
            print(f"Traceback: {traceback.format_exc()}", flush=True)
            raise Exception(error_msg)