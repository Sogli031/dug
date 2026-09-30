
filename = r"c:\Users\PC\Desktop\programi\dug\app\src\main\java\com\example\debttracker\MainActivity.kt"

with open(filename, 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

balance = 0
stack = []

for i, line in enumerate(lines):
    for char in line:
        if char == '{':
            balance += 1
            stack.append(i + 1)
        elif char == '}':
            balance -= 1
            if stack:
                stack.pop()

if balance != 0:
    print(f"Brace balance is {balance} (positive means missing }}, negative means extra }})")
    if balance > 0:
        print(f"Unclosed braces opened at lines: {stack[-5:]}")
else:
    print("Braces are balanced.")
