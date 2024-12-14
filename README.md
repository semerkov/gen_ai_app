# Getting Started

## 1. Environment variables
Set the following environment variables:
* OPEN_AI_DEPLOYMENT_NAME - deployment name (e.g. gpt-35-turbo);
* OPEN_AI_ENDPOINT - service endpoint that will be connected to by client;
* OPEN_AI_KEY - API key used to authorize requests.

## 2. Run Spring Boot application

## 3. Endpoints

### 3.1 Open AI

POST http://localhost:8085/prompt/open-ai/send

Example of request:
```json
{
    "input": "What is the semantic kernel in the context of working with LLM?"
}
```

Example of response:
```json
{
    "answers": [
        "In the context of working with LLM (Language Model for Language Modeling), the semantic kernel refers to the underlying representation or understanding of the meaning and relations between words and concepts in a given text. It is a fundamental component of LLM models, as it helps capture the semantic information and enables the model to generate coherent and contextually appropriate responses. The semantic kernel is responsible for mapping the input text into a numerical representation that the model can process and use for generating text."
    ]
}
```

### 3.2 Semantic Kernel

POST http://localhost:8085/prompt/sk/send

Example of request:
```json
{
    "input": "What is the semantic kernel?"
}
```

Example of response:
```json
{
    "answers": [
        "The term \"semantic kernel\" refers to the core or essential meaning of a piece of information or text. It represents the central ideas, concepts, or key information that gives the text its overall meaning. The semantic kernel focuses on the fundamental messages or main points conveyed by the information, disregarding less significant or peripheral details.\n\nIn natural language processing, the semantic kernel plays a crucial role in various language-related tasks such as text summarization, information retrieval, and understanding context. By identifying the semantic kernel, one can distill the most important information from a text or document, enabling more efficient and effective analysis, interpretation, and communication of the underlying meaning."
    ]
}
```

### 3.3 Get common information about place (Semantic Kernel Plugin)

POST http://localhost:8085/prompt/sk/place/commonInfo

Example of request:
```json
{
    "input": "Could you provide currency exchange rate for The Netherlands and weather for the nearest 5 days?"
}
```

Example of response:
```json
{
    "answers": [
        "The currency exchange rate for the Netherlands is 1 EUR = 4.2722 PLN.\n\nHere is the weather forecast for Amsterdam for the next 5 days:\n\n- Day 1 (2024-12-14): The temperature will range from 0.9°C to 7.8°C.\n- Day 2 (2024-12-15): The temperature will range from 5.7°C to 12.2°C.\n- Day 3 (2024-12-16): The temperature will range from 4.0°C to 11.9°C.\n- Day 4 (2024-12-17): The temperature will range from 4.7°C to 12.1°C.\n- Day 5 (2024-12-18): The temperature will range from 6.7°C to 12.1°C.\n\nPlease note that weather forecasts are subject to change, and it's always a good idea to check for updates closer to the date."
    ]
}
```