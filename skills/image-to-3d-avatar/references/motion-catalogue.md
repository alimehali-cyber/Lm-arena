# Motion catalogue — AssetFurnace's 21 presets

Read from `image_to_3dlab/motions/presets.json` (schema_version 1) at commit `1a50935`. Each move
ships as a `.npz` clip plus a "take B" variant (`_b` suffix), which is a second performance of the
same direction — useful when take A's timing fights the character.

**Licence:** "Clips made with NVIDIA Kimodo (NVIDIA Open Model License). NVIDIA claims no ownership of
outputs." Credit Kimodo wherever the clips ship; check the licence text before redistributing the raw
`.npz` files (they are not vendored into this repository).

| id | label | seconds | direction given to Kimodo |
|---|---|---|---|
| `idle` | Idle | 4.0 | A person stands still and breathes calmly, looking around. |
| `idle_b` | Idle (take B) | 4.0 | same |
| `walk` | Walk | 4.0 | A person walks forward. |
| `walk_b` | Walk (take B) | 4.0 | same |
| `walk_woman` | Walk (woman) | 4.0 | A woman walks forward. |
| `run` | Run | 3.0 | A person runs forward. |
| `run_b` | Run (take B) | 3.0 | same |
| `jump` | Jump | 2.5 | A person jumps up in place. |
| `jump_b` | Jump (take B) | 2.5 | same |
| `wave` | Wave | 3.0 | A person waves with their right hand. |
| `wave_b` | Wave (take B) | 3.0 | same |
| `punch` | Punch | 2.0 | A person throws a punch with their right fist. |
| `punch_b` | Punch (take B) | 2.0 | same |
| `kick` | Kick | 2.0 | A person kicks forward with their right leg. |
| `kick_b` | Kick (take B) | 2.0 | same |
| `sword_swing` | Sword swing | 2.5 | A person swings a sword overhead with both hands. |
| `sword_swing_b` | Sword swing (take B) | 2.5 | same |
| `dance` | Dance | 5.0 | A person dances happily. |
| `dance_b` | Dance (take B) | 5.0 | same |
| `sit` | Sit down | 3.0 | A person sits down on the ground. |
| `sit_b` | Sit down (take B) | 3.0 | same |
| `sneak` | Sneak | 4.0 | A person sneaks forward in a crouch. |
| `sneak_b` | Sneak (take B) | 4.0 | same |
| `strafe_left` | Strafe left | 3.0 | A person sidesteps to the left. |
| `strafe_left_b` | Strafe left (take B) | 3.0 | same |
| `death` | Death | 3.0 | A person falls backward to the ground and lies still. |
| `death_b` | Death (take B) | 3.0 | same |
| `hit_react` | Hit react | 2.0 | A person gets hit in the chest and staggers backward. |
| `hit_react_b` | Hit react (take B) | 2.0 | same |
| `cheer` | Cheer | 3.0 | A person raises both arms in the air and cheers. |
| `cheer_b` | Cheer (take B) | 3.0 | same |
| `roll` | Roll | 3.0 | A person does a forward roll on the ground. |
| `roll_b` | Roll (take B) | 3.0 | same |
| `kata` | Kata | 5 | A person performs a martial arts kata with fast punches and blocks. |
| `kata_b` | Kata (take B) | 5 | same |
| `roundhouse` | Roundhouse kick | 2.5 | A person does a high roundhouse kick. |
| `roundhouse_b` | Roundhouse kick (take B) | 2.5 | same |
| `flying_kick` | Flying kick | 2.5 | A person jumps and does a flying kick. |
| `flying_kick_b` | Flying kick (take B) | 2.5 | same |
| `fight_stance` | Fighting stance | 4 | A person bounces lightly on their feet in a fighting stance. |
| `fight_stance_b` | Fighting stance (take B) | 4 | same |
| `punch_combo` | Punch combo | 3 | A person throws a fast combination of punches. |
| `punch_combo_b` | Punch combo (take B) | 3 | same |

## What the Home scene actually needs

None of these is a "stand on a grassy ridge and look up at the sky" clip, and none is a "shade your
eyes at noon". The scene's three beats (day / twilight / night) are poses, so they are authored or
blended on top of `idle`:

| Clip (our name) | Built from | Loop | Notes |
|---|---|---|---|
| `idle_breathe` | authored | 8 s, seamless | breath 4 s + weight shift 8 s, head drift |
| `day_shade` | authored (IK) | 8 s | wrist to the brow, elbow out, other hand on the hip |
| `dusk_phone` | authored (IK) | 8 s | both wrists meet at the chest, head down 6–8° |
| `night_point` | authored (IK) | 8 s | right wrist above the head, head up 10°, chest open |
| `wave_hello` | authored (IK) | 3 s, once | the one generic clip worth keeping for fun |

Rule of thumb for the scene: **the character is 170 px tall**. Any motion that reads as a 1° change at
full body is invisible; exaggerate the silhouette, not the detail. Slow it down: a home-screen
character that fidgets every second is noise.
