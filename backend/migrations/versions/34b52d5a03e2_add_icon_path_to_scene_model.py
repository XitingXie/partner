"""Add icon_path to Scene model

Revision ID: 34b52d5a03e2
Revises: bc80f603987c
Create Date: 2025-01-30 13:36:05.257500

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '34b52d5a03e2'
down_revision = 'bc80f603987c'
branch_labels = None
depends_on = None


def upgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    op.create_table('scene_level',
    sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
    sa.Column('scene_id', sa.Integer(), nullable=False),
    sa.Column('english_level', sa.String(length=2), nullable=False),
    sa.Column('example_dialogs', sa.Text(), nullable=True),
    sa.Column('key_phrases', sa.Text(), nullable=True),
    sa.Column('vocabulary', sa.Text(), nullable=True),
    sa.Column('grammar_points', sa.Text(), nullable=True),
    sa.Column('created_at', sa.DateTime(), nullable=True),
    sa.ForeignKeyConstraint(['scene_id'], ['scene.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    with op.batch_alter_table('scene', schema=None) as batch_op:
        batch_op.add_column(sa.Column('icon_path', sa.String(length=255), nullable=True))
        batch_op.drop_column('key_phrases')
        batch_op.drop_column('example_dialogs')

    with op.batch_alter_table('topic', schema=None) as batch_op:
        batch_op.add_column(sa.Column('icon_path', sa.String(length=255), nullable=True))
        batch_op.drop_column('keywords')

    # ### end Alembic commands ###


def downgrade():
    # ### commands auto generated by Alembic - please adjust! ###
    with op.batch_alter_table('topic', schema=None) as batch_op:
        batch_op.add_column(sa.Column('keywords', sa.TEXT(), nullable=True))
        batch_op.drop_column('icon_path')

    with op.batch_alter_table('scene', schema=None) as batch_op:
        batch_op.add_column(sa.Column('example_dialogs', sa.TEXT(), nullable=True))
        batch_op.add_column(sa.Column('key_phrases', sa.TEXT(), nullable=True))
        batch_op.drop_column('icon_path')

    op.drop_table('scene_level')
    # ### end Alembic commands ###
