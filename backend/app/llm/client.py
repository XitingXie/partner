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
        print("Initializing LLM client ", flush=True)
        self.client = OpenAI(
            api_key=os.getenv('OPENAI_API_KEY')
        )

    def get_completion(self, prompt: str, message: str, temperature: float = 0.7, role: str = "partner") -> str:
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
            print(f"Prompt length: {len(prompt)}", flush=True)
            print(f"Message length: {len(message)}", flush=True)
            print(f"Temperature: {temperature}", flush=True)

            # Truncate prompt and message if they're extremely long
            truncated_prompt = prompt[:5000] if len(prompt) > 5000 else prompt
            truncated_message = message[:1000] if len(message) > 1000 else message

            if role == "partner":
                response = self.client.chat.completions.create(
                    model="gpt-4o-mini",
                    messages=[
                        {"role": "system", "content": truncated_prompt},
                        {"role": "user", "content": truncated_message}
                    ],
                    temperature=temperature,
                    stream=False,
                    max_tokens=50,
                    timeout=30.0
                )
            else:
                response = self.client.chat.completions.create(
                    model="gpt-4o-mini",
                    messages=[
                        {"role": "system", "content": truncated_prompt},
                        {"role": "user", "content": truncated_message}
                    ],
                    temperature=temperature,
                    stream=False,
                    timeout=30.0
                )
            
            print(f"\nAPI Response received", flush=True)
            ai_response = response.choices[0].message.content
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