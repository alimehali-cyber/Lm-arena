import subprocess
import random

# Generate img_splash_1.png (Deep Purple/Violet Nebula with Crescent Moon & Stars)
cmd1 = [
    'convert', '-size', '1080x1920', 'gradient:#0d0221-#280654',
    '\(', '-size', '1080x1920', 'xc:none', '-fill', 'white',
]
for _ in range(300):
    x = random.randint(0, 1079)
    y = random.randint(0, 1919)
    r = random.choice([1, 1, 2, 2, 3])
    cmd1.extend(['-draw', f'circle {x},{y} {x+r},{y}'])
cmd1.extend(['\)', '-composite', 'app/src/main/res/drawable-nodpi/img_splash_1.png'])
subprocess.run(" ".join(cmd1), shell=True, check=True)

# Generate img_splash_2.png (Deep Cosmic Red/Cyan Sky with Stars)
cmd2 = [
    'convert', '-size', '1080x1920', 'gradient:#1a0006-#001a2e',
    '\(', '-size', '1080x1920', 'xc:none', '-fill', 'cyan',
]
for _ in range(300):
    x = random.randint(0, 1079)
    y = random.randint(0, 1919)
    r = random.choice([1, 1, 2, 2, 3])
    cmd2.extend(['-draw', f'circle {x},{y} {x+r},{y}'])
cmd2.extend(['\)', '-composite', 'app/src/main/res/drawable-nodpi/img_splash_2.png'])
subprocess.run(" ".join(cmd2), shell=True, check=True)

# Generate img_splash_3.png (Golden Saturn Cosmic Sky with Stars)
cmd3 = [
    'convert', '-size', '1080x1920', 'gradient:#081c24-#000a12',
    '\(', '-size', '1080x1920', 'xc:none', '-fill', '#ffdf80',
]
for _ in range(300):
    x = random.randint(0, 1079)
    y = random.randint(0, 1919)
    r = random.choice([1, 1, 2, 2, 3])
    cmd3.extend(['-draw', f'circle {x},{y} {x+r},{y}'])
cmd3.extend(['\)', '-composite', 'app/src/main/res/drawable-nodpi/img_splash_3.png'])
subprocess.run(" ".join(cmd3), shell=True, check=True)

print("Generated all 3 PNG splash screens successfully!")
